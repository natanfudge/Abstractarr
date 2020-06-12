import api.*
import codegeneration.*
import descriptor.JvmPrimitiveType
import signature.*
import util.*
import java.nio.file.Path

data class AbstractionMetadata(val versionPackage: String, val classPath: List<Path>)

//TODO:
// baseclasses
// annotations (nullable etc)
// test inner class constructors with throws, supposedly signatures don't include synthetic parameters
// test anon classes or lambdas
// @NoNull on all .create() functions
// throws

//TODO: deal with cases such as these:
//class McClass1<T extends JdkClass>
//class MCClass2 extends JdkClass
//class McClass3 extends McClass1<McClass2>
//
//-- >
//interface IMcClass1<T extends JdkClass>
//interface IMCClass2 extends SuperTyped<JdkClass>
//interface IMcClass3 extends SuperTyped<IMcClass2> // boom!!! IMcClass2 does not extend JdkClass

object Abstractor {
    fun abstract(mcJar: Path, destDir: Path, metadata: AbstractionMetadata) {
        require(destDir.parent.exists()) { "The chosen destination path '$destDir' is not in any existing directory." }
        require(destDir.parent.isDirectory()) { "The parent of the chosen destination path '$destDir' is not a directory." }

        destDir.deleteRecursively()
        destDir.createDirectory()

        val index = indexClasspath(metadata.classPath + listOf(mcJar) /*+ getJvmBootClasses()*/)

        for (classApi in ClassApi.readFromJar(mcJar)/*.filter { it.name.shortName.startsWith("TestGenerics") }*/) {
            if (!classApi.isPublicApi) continue
            ClassAbstractor(metadata, index, classApi).abstractClass(destDir, outerClass = null)
        }
    }
}


private class ClassAbstractor(
    private val metadata: AbstractionMetadata,
    private val index: ClasspathIndex,
    private val classApi: ClassApi
) {

    /**
     * destPath == null and outerClass != null when it's an inner class
     */
    fun abstractClass(destPath: Path?, outerClass: JavaGeneratedClass?) {
        check(classApi.visibility == ClassVisibility.Public)

        // Add SuperTyped<SuperClass> superinterface for classes which have a non-mc superclass
        val superTypedInterface = getSuperTypedInterface()

        // No need to add I for inner classes
        val (packageName, shortName) = if (outerClass == null) classApi.name.toApiClass() else classApi.name
        val classInfo = ClassInfo(
            visibility = classApi.visibility,
            isAbstract = false,
            isInterface = true,
            shortName = shortName.innermostClass(),
            typeArguments = allApiClassTypeArguments(),
            superInterfaces = classApi.superInterfaces.remapToApiClasses().appendIfNotNull(superTypedInterface),
            superClass = null
        ) { addClassBody() }

        if (destPath != null) {
            JavaCodeGenerator.writeClass(classInfo, packageName, destPath)
        } else {
            requireNotNull(outerClass)
            outerClass.addInnerClass(classInfo, isStatic = true)
        }
    }

    private fun getSuperTypedInterface() = classApi.superClass?.let {
        when {
            it.isMcClass() -> it.remapToApiClass()
            it.satisfyingTypeBoundsIsImpossible() -> null
            else -> ClassGenericType(
                SuperTypedPackage,
                SimpleClassGenericType(
                    SuperTypedName,
                    TypeArgument.SpecificType(
                        /*if (it.containsMcClasses()) it.remapToApiClass().type else it.type*/ it.remapToApiClass().type,
                        wildcardType = null
                    ).singletonList()
                ).singletonList()
            ).noAnnotations()
        }

    }


    // TODO: This will do for now. A proper solution will check if:
    // 1. This is used as a superclass in this context (this is already the case when this method is called)
    // 2. This is a non-mc class
    // 3. Any type arguments are bound by a type that contains themselves (e.g. T extends Enum<T>)
    // ALTERNATIVELY------- instead of generating a SuperTyped<> interfaces just add the cast method itself to the class
    // OR do some bytecode shenanigans if that works
    private fun JavaClassType.satisfyingTypeBoundsIsImpossible() = type.packageName == EnumPackage
            && type.classNameSegments[0].name == EnumName


    private fun JavaGeneratedClass.addClassBody() {
        for (method in classApi.methods) {
            abstractMethod(method)
        }

        for (field in classApi.fields) {
            abstractField(field)
        }

        for (innerClass in classApi.innerClasses) {
            if (!innerClass.isPublic) continue
            ClassAbstractor(metadata, index, innerClass).abstractClass(destPath = null, outerClass = this)

            // Inner classes are constructed by their parent
            if (!innerClass.isStatic) {
                addInnerClassConstructor(innerClass)
            }
        }

        addArrayFactory()
    }

    private fun JavaGeneratedClass.abstractMethod(method: ClassApi.Method) {
        // Only public methods are abstracted
        if (!method.isPublic) return
        // Abstract classes/interfaces can't be constructed directly
        if (method.isConstructor && (classApi.isInterface || classApi.isAbstract)) return
        // Inner classes need to be constructed by their parent class
        if (classApi.isInnerClass && method.isConstructor && !classApi.isStatic) return

        // Don't duplicate methods that are just being overriden
        if (method.isOverride(index,classApi)) return

        val parameters = apiParametersDeclaration(method)

        val mcReturnType =
            if (method.isConstructor) classApi.asType().pushAllTypeArgumentsToInnermostClass() else method.returnType
        val returnType = mcReturnType.remapToApiClass()

        val mcClassType = classApi.asRawType()

        addMethod(
            name = if (method.isConstructor) "create" else method.name,
            visibility = method.visibility, parameters = parameters,
            final = false,
            static = method.isStatic || method.isConstructor,
            abstract = false,
            returnType = returnType,
            typeArguments = if (method.isConstructor) allApiClassTypeArguments() else method.typeArguments.remapDeclToApiClasses()
        ) {
            val passedParameters = apiPassedParameters(method)
            val call = if (method.isConstructor) {
                Expression.Call.Constructor(
                    constructing = mcClassType,
                    parameters = passedParameters,
                    receiver = null
                )
            } else {
                Expression.Call.Method(
                    receiver = if (method.isStatic) ClassReceiver(mcClassType)
                    else Expression.This.castTo(mcClassType),
                    parameters = passedParameters,
                    name = method.name
                )
            }

            addStatement(
                if (!method.isVoid || method.isConstructor) {
                    check(mcReturnType.type is GenericTypeOrPrimitive) // because it's not void
                    @Suppress("UNCHECKED_CAST")
                    val returnedValue = call.castFromMcToApiClass(mcReturnType as JavaType<GenericTypeOrPrimitive>)
                    Statement.Return(returnedValue)
                } else call
            )
        }
    }

    private fun JavaGeneratedClass.abstractField(field: ClassApi.Field) {
        if (!field.isPublic) return
        if (field.isFinal && field.isStatic) {
            addConstant(field)
        } else {
            addGetter(field)
        }

        if (!field.isFinal) {
            addSetter(field)
        }
    }

    private fun JavaGeneratedClass.addConstant(field: ClassApi.Field) {
        addField(
            name = field.name, final = true, static = true, visibility = Visibility.Public,
            type = field.type.remapToApiClass(),
            initializer = abstractedFieldExpression(field).castFromMcToApiClass(field.type)
        )
    }

    private fun JavaGeneratedClass.addGetter(field: ClassApi.Field) {
        val getterName = field.getGetterPrefix() +
                // When it starts with "is" no prefix is added so there's no need to capitalize
                if (field.name.startsWith(BooleanGetterPrefix)) field.name else field.name.capitalize()
        addMethod(
            // Add _field when getter clashes with a method of the same name
            name = if (classApi.methods.any { it.parameters.isEmpty() && it.name == getterName }) getterName + "_field"
            else getterName,
            parameters = mapOf(),
            visibility = Visibility.Public,
            returnType = field.type.remapToApiClass(),
            abstract = false,
            static = field.isStatic,
            final = false,
            typeArguments = listOf()
        ) {
            val fieldAccess = abstractedFieldExpression(field)

            addStatement(
                Statement.Return(
                    fieldAccess.castFromMcToApiClass(field.type)
                )
            )
        }
    }

    private fun ClassApi.Field.getGetterPrefix(): String =
        if (type.type.let { it is GenericsPrimitiveType && it.primitive == JvmPrimitiveType.Boolean }) {
            if (name.startsWith(BooleanGetterPrefix)) "" else BooleanGetterPrefix
        } else "get"

    private fun JavaGeneratedClass.addSetter(field: ClassApi.Field) {
        addMethod(
            name = "set" + field.name.capitalize(),
            parameters = mapOf(field.name to field.type.remapToApiClass()),
            visibility = Visibility.Public,
            returnType = GenericReturnType.Void.noAnnotations(),
            abstract = false,
            static = field.isStatic,
            final = false,
            typeArguments = listOf()
        ) {
            addStatement(
                Statement.Assignment(
                    target = abstractedFieldExpression(field),
                    assignedValue = Expression.Variable(field.name).castToMcClass(field.type)
                )
            )
        }
    }

    private fun abstractedFieldExpression(
        field: ClassApi.Field
    ): Expression.Field {
        val mcClassType = classApi.asRawType()
        return Expression.Field(
            owner = if (field.isStatic) ClassReceiver(mcClassType) else Expression.This.castTo(mcClassType),
            name = field.name
        )
    }

    private fun JavaGeneratedClass.addInnerClassConstructor(innerClass: ClassApi) {
        for (constructor in innerClass.methods.filter { it.isConstructor }) {
            val constructedInnerClass = innerClass.asType()
            addMethod(
                name = "new" + innerClass.name.shortName.innermostClass(),
                visibility = Visibility.Public,
                static = false,
                final = false,
                abstract = false,
                returnType = constructedInnerClass.remapToApiClass().pushAllTypeArgumentsToInnermostClass(),
                parameters = apiParametersDeclaration(constructor)
                    //no need for the this$0 outer class reference
                    .toList().drop(1).toMap(),
                typeArguments = innerClass.typeArguments.remapDeclToApiClasses()
            ) {
                addStatement(
                    Statement.Return(
                        Expression.Call.Constructor(
                            constructing = innerClass.innerMostClassNameAsType(),
                            parameters = apiPassedParameters(constructor)
                                //no need for the this$0 outer class reference
                                .drop(1),
                            receiver = Expression.This.castTo(classApi.asRawType())
                        ).castFromMcToApiClass(constructedInnerClass, doubleCast = doubleCastRequired(innerClass))
                    )
                )
            }
        }
    }

    private fun apiPassedParameters(method: ClassApi.Method): List<Expression> =
        method.parameters
            .map { (name, type) -> Expression.Variable(name).castToMcClass(type, doubleCast = true) }


    private fun apiParametersDeclaration(method: ClassApi.Method) =
        method.parameters.mapValues { (_, v) -> v.remapToApiClass() }.toMap()

    private fun JavaGeneratedClass.addArrayFactory() {
        addMethod(
            name = if (existsMethodWithSameDescriptorAsArrayFactory()) "${ArrayFactoryName}_factory" else ArrayFactoryName,
            visibility = Visibility.Public,
            parameters = mapOf(ArrayFactorySizeParamName to GenericsPrimitiveType.Int.noAnnotations()),
            returnType = ArrayGenericType(classApi.asType().type).noAnnotations().remapToApiClass()
                .pushAllArrayTypeArgumentsToInnermostClass(),
            abstract = false,
            final = false,
            static = true,
            typeArguments = allApiClassTypeArguments()
        ) {
            addStatement(
                Statement.Return(
                    Expression.ArrayConstructor(
                        classApi.asRawType(),
                        size = Expression.Variable(ArrayFactorySizeParamName)
                    )
                )
            )
        }
    }

    private fun existsMethodWithSameDescriptorAsArrayFactory() = classApi.methods
        .any { method ->
            method.name == ArrayFactoryName && method.parameters.size == 1
                    && method.parameters.values.first().type.let { it == GenericsPrimitiveType.Int }
        }

    internal fun allApiClassTypeArguments(): List<TypeArgumentDeclaration> = if (classApi.isStatic) {
        classApi.typeArguments.remapDeclToApiClasses()
    } else classApi.listInnerClassChain().flatMap { it.typeArguments.remapDeclToApiClasses() }



    ////////// REMAPPING /////////

    private fun PackageName?.toApiPackageName() = metadata.versionPackage.prependToQualified(this ?: PackageName.Empty)
    private fun ShortClassName.toApiShortClassName() =
        ShortClassName(("I" + outerClass()).prependTo(innerClasses()))

    private fun QualifiedName.toApiClass(): QualifiedName = if (isMcClassName()) {
        QualifiedName(
            packageName = packageName.toApiPackageName(),
            shortName = shortName.toApiShortClassName()
        )
    } else this

    private fun <T : GenericReturnType> JavaType<T>.remapToApiClass(): JavaType<T> = remap { it.toApiClass() }
    private fun <T : GenericReturnType> T.remapToApiClass(): T = remap { it.toApiClass() }
    private fun List<TypeArgumentDeclaration>.remapDeclToApiClasses() = map {
        it.copy(
            classBound = it.classBound?.remapToApiClass(),
            interfaceBounds = it.interfaceBounds.map { it.remapToApiClass() })
    }


    private fun <T : GenericReturnType> List<JavaType<T>>.remapToApiClasses(): List<JavaType<T>> =
        map { it.remapToApiClass() }

    /////////// CASTING //////////

    /**
     * For any type Child, and type Super, such that Super is a supertype of Child:
     * Casting from Child[] to Super[] is valid,
     * Casting from Super[] to Child[] will always fail.
     */


    // Since api classes are superinterfaces of mc classes, in most cases a cast is not required.
    private fun JavaType<*>.castRequiredToApiClass(): Boolean = when {
        type is TypeVariable -> true
        type !is ClassGenericType && type !is ArrayGenericType -> castRequiredToMcClass()
        else -> type.getContainedClassesRecursively()
            // Don't include the top-level class type (or in arrays, the class type in the top level array)
            .filter {
                when (val type = type) {
                    is ClassGenericType -> it != type
                    is ArrayGenericType -> it != type.componentType
                    else -> error("impossible")
                }
            }
            .any { it.packageName.isMcPackage() }
    }

    private fun Expression.castFromMcToApiClass(type: AnyJavaType, doubleCast: Boolean? = null): Expression =
        if (type.castRequiredToApiClass()) this.castTo(type.remapToApiClass(), doubleCast) else this


    private fun Expression.castToMcClass(type: AnyJavaType, doubleCast: Boolean? = null): Expression =
        if (type.castRequiredToMcClass()) castTo(type, doubleCast) else this

    private fun JavaType<*>.castRequiredToMcClass(): Boolean =
        type is TypeVariable || type.getContainedClassesRecursively()
            .any { it.packageName.isMcPackage() }

    fun Expression.castTo(type: AnyJavaType, forceDoubleCast: Boolean? = null): Expression =
        castExpressionTo(type, forceDoubleCast ?: doubleCastRequired(classApi))

}

private val SuperTypedPackage = PackageName(listOf("febb", "apiruntime"))
private const val SuperTypedName = "SuperTyped"
private val EnumPackage = PackageName(listOf("java", "lang"))
private const val EnumName = "Enum"

private const val BooleanGetterPrefix = "is"
private const val ArrayFactoryName = "array"
private const val ArrayFactorySizeParamName = "size"