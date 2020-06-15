import abstractor.VersionPackage
import abstractor.isMcClass
import abstractor.isMcPackage
import api.*
import codegeneration.*
import descriptor.JvmPrimitiveType
import descriptor.JvmType
import descriptor.ObjectType
import signature.*
import util.*
import java.nio.file.Path

data class AbstractionMetadata(
    val versionPackage: VersionPackage,
    val classPath: List<Path>,
    val includeImplementationDetails: Boolean
)


//TODO:
// baseclasses
//TODO: list of wanted asm magic:
// - jdk interfaces of mc classes
// - Overloads with different return type (i.e. methods that the only mc class in it is in the return type)
// - Remove casts
// - Final interfaces
// - Add type bounds without them being checked (enums, other supertyped cases)
// - Maybe all generics in general should be with asm?
// i.e.:
//class McClass1<T extends JdkClass>
//class MCClass2 extends JdkClass
//class McClass3 extends McClass1<McClass2>
//
//-- >
//interface IMcClass1<T cextends JdkClass>
//interface IMCClass2 extends SuperTyped<JdkClass>
//interface IMcClass3 extends SuperTyped<IMcClass2> // boom!!! IMcClass2 does not extend JdkClass

object Abstractor {
    fun abstract(mcJar: Path, destDir: Path, metadata: AbstractionMetadata) {
        require(destDir.parent.exists()) { "The chosen destination path '$destDir' is not in any existing directory." }
        require(destDir.parent.isDirectory()) { "The parent of the chosen destination path '$destDir' is not a directory." }

        destDir.deleteRecursively()
        destDir.createDirectory()

        val index = indexClasspath(metadata.classPath + listOf(mcJar) /*+ getJvmBootClasses()*/)

        for (classApi in ClassApi.readFromJar(mcJar)
//            .filter { it.name.shortName.startsWith("TestGenerics") }
        ) {
            if (!classApi.isPublicApi) continue
            ClassAbstractor(metadata, index, classApi, baseClass = false).abstractClass(
                destPath = destDir,
                outerClass = null
            )
            if (!classApi.isFinal) {
                ClassAbstractor(metadata, index, classApi, baseClass = true).abstractClass(
                    destPath = destDir,
                    outerClass = null
                )
            }
        }
    }
}


private data class ClassAbstractor(
    private val metadata: AbstractionMetadata,
    private val index: ClasspathIndex,
    private val classApi: ClassApi,
    private val baseClass: Boolean
) {

    /**
     * destPath == null and outerClass != null when it's an inner class
     */
    fun abstractClass(outerClass: GeneratedClass?, destPath: Path?) = with(metadata.versionPackage) {
        //TODO: better solution for generics
        if (baseClass && classApi.typeArguments.isNotEmpty()) return@with
        check(classApi.visibility == ClassVisibility.Public)
        // No need to add I for inner classes
        val (packageName, shortName) = if (outerClass == null) {
            if (baseClass) classApi.name.toBaseClass() else classApi.name.toApiClass()
        } else classApi.name

        val isInterface = when {
            !baseClass -> true
            classApi.isInterface -> true
            else -> false
        }

        val mcClass = classApi.asType()

        val interfaces = if (baseClass) {
            listOf(
                mcClass.remapToApiClass().pushAllTypeArgumentsToInnermostClass()
            ).applyIf(isInterface) { it + mcClass }
        } else {
            // Add SuperTyped<SuperClass> superinterface for classes which have a non-mc superclass
            val superTypedInterface = getSuperTypedInterface()
            //TODO: in the distributed version, add in the jdk interfaces
            classApi.superInterfaces.filter { it.isMcClass() }.remapToApiClasses().appendIfNotNull(superTypedInterface)
        }

        val classInfo = ClassInfo(
            visibility = ClassVisibility.Public,
            access = ClassAccess(
                isFinal = false,
                variant = if (isInterface) ClassVariant.Interface else ClassVariant.AbstractClass
            ),
            shortName = shortName.innermostClass(),
            typeArguments = allApiClassTypeArguments(),
            superInterfaces = interfaces,
            superClass = if (baseClass && !isInterface) classApi.asRawType() else null,
            annotations = /*classApi.annotations*/ listOf() // Translating annotations can cause compilation errors...
        ) { addClassBody() }

        if (destPath != null) {
            JavaCodeGenerator.writeClass(classInfo, packageName, destPath)
        } else {
            requireNotNull(outerClass)
            outerClass.addInnerClass(classInfo, isStatic = !baseClass || classApi.isStatic)
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


    private fun GeneratedClass.addClassBody() {
        for (method in classApi.methods) {
            abstractMethod(method)
        }

        if (!baseClass) {
            for (field in classApi.fields) {
                abstractField(field)
            }
        }
        for (innerClass in classApi.innerClasses) {
            if (!innerClass.isPublic) continue
            this@ClassAbstractor.copy(classApi = innerClass).abstractClass(destPath = null, outerClass = this)

            // Inner classes are constructed by their parent
            if (!baseClass && !innerClass.isStatic) {
                addInnerClassConstructor(innerClass)
            }
        }

        if (!baseClass) addArrayFactory()

    }
    //TODO: pull out the baseclass boolean and make it a whole new thing I think

    private fun GeneratedClass.abstractMethod(method: ClassApi.Method) {
        // Only public methods are abstracted
        if (!method.isPublic) return
        if (baseClass) {
            // Baseclasses only exist for you to override methods
            if (method.isFinal || method.isStatic) return
            // TODO: there's no real way to overload methods that only differ in return type, at least in java
            if (/*method.returnType.castRequiredToMcClass() &&*/ method.parameters.values.none { it.castRequiredToMcClass() }) return
            // Abstract baseclass methods are abstract too, but since the api interface already declares the method there's no need to declare it again
            if (method.isAbstract) return
            if (!metadata.includeImplementationDetails) return
        }
        // Abstract classes/interfaces can't be constructed directly
        if (method.isConstructor && (classApi.isInterface || (!baseClass && classApi.isAbstract))) return
        // Inner classes need to be constructed by their parent class in api interfaces
        if (!baseClass && classApi.isInnerClass && method.isConstructor && !classApi.isStatic) return

//        if (method.isOverride(index, classApi)) return
//        if (baseClass) {
        // Don't duplicate methods that are just being overriden
        // In baseclasses generic types are just Object, so overriding with a superclass return type usually fails,
        // so we remove such overrides.
        if (!baseClass && method.isOverrideIgnoreReturnType(index, classApi)) return
//        }
        //TODO: test if baseclass and method with object return type is override

        val mcReturnType = if (method.isConstructor) {
            val type = if (baseClass) classApi.asType() else classApi.asType().pushAllTypeArgumentsToInnermostClass()
            type.copy(annotations = listOf(NotNullAnnotation))
        } else method.returnType
        val returnType = if (baseClass) mcReturnType else mcReturnType.remapToApiClass()

        val mcClassType = classApi.asRawType()

        val methodInfo = MethodInfo(
            visibility = method.visibility,
            throws = method.throws.map { it.remapToApiClass() },
            parameters = if (baseClass) method.parameters else apiParametersDeclaration(method)
        ) {
            val passedParameters = apiPassedParameters(method)
            val statement = if (method.isConstructor && baseClass) Statement.ConstructorCall.Super(passedParameters)
            else {
                val call = if (method.isConstructor) {
                    Expression.Call.Constructor(
                        constructing = mcClassType,
                        parameters = passedParameters,
                        receiver = null
                    )
                } else {
                    Expression.Call.Method(
                        receiver = when {
                            method.isStatic -> ClassReceiver(mcClassType)
                            baseClass -> /*Expression.This*/ null
                            else -> Expression.This.castTo(mcClassType)
                        },
                        parameters = if (baseClass) method.parameters.map { (name, type) ->
                            type.toJvmType() to Expression.Variable(name)
                                .castFromMcToApiClass(type, doubleCast = true, forceCast = true)
                        }
                        else passedParameters,
                        name = method.name,
                        methodAccess = method.access,
                        receiverAccess = classApi.access,
                        returnType = (if (baseClass) returnType else mcReturnType).toJvmType(),
                        owner = (if (baseClass) mcClassType.remapToApiClass() else mcClassType).toJvmType()
                    )
                }

                if (!method.isVoid || method.isConstructor) {
                    check(mcReturnType.type is GenericTypeOrPrimitive) // because it's not void
                    @Suppress("UNCHECKED_CAST")
                    val returnedValue =
                        if (baseClass) call.castToMcClass(returnType as JavaType<GenericTypeOrPrimitive>) else
                            call.castFromMcToApiClass(mcReturnType as JavaType<GenericTypeOrPrimitive>)
                    Statement.Return(returnedValue)
                } else call
            }

            addStatement(statement)
        }

        if (method.isConstructor && baseClass) {
            addConstructor(methodInfo)
        } else {
            addMethod(
                methodInfo,
                name = if (method.isConstructor) "create" else method.name,
                access = MethodAccess(
                    isFinal = baseClass && !classApi.isInterface,
                    isStatic = method.isStatic || method.isConstructor,
                    isAbstract = false
                ),
                returnType = returnType.applyIf(baseClass) { it.copy(annotations = it.annotations + OverrideAnnotation) },
                typeArguments = if (method.isConstructor) allApiClassTypeArguments() else method.typeArguments.remapDeclToApiClasses()
            )
        }
    }

    private fun GeneratedClass.abstractField(field: ClassApi.Field) {
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

    private fun GeneratedClass.addConstant(field: ClassApi.Field) {
        addField(
            name = field.name, isFinal = true, isStatic = true, visibility = Visibility.Public,
            type = field.type.remapToApiClass(),
            initializer = abstractedFieldExpression(field).castFromMcToApiClass(field.type)
        )
    }

    private fun GeneratedClass.addGetter(field: ClassApi.Field) {
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
            typeArguments = listOf(),
            throws = listOf()
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

    private fun GeneratedClass.addSetter(field: ClassApi.Field) {
        addMethod(
            name = "set" + field.name.capitalize(),
            parameters = mapOf(field.name to field.type.remapToApiClass()),
            visibility = Visibility.Public,
            returnType = GenericReturnType.Void.noAnnotations(),
            abstract = false,
            static = field.isStatic,
            final = false,
            typeArguments = listOf(),
            throws = listOf()
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

    private fun GeneratedClass.addInnerClassConstructor(innerClass: ClassApi) {
        for (constructor in innerClass.methods.filter { it.isConstructor }) {
            val constructedInnerClass = innerClass.asType()
            addMethod(
                name = "new" + innerClass.name.shortName.innermostClass(),
                visibility = Visibility.Public,
                static = false,
                final = false,
                abstract = false,
                returnType = constructedInnerClass.remapToApiClass().pushAllTypeArgumentsToInnermostClass()
                    .copy(annotations = listOf(NotNullAnnotation)),
                parameters = apiParametersDeclaration(constructor),
                typeArguments = innerClass.typeArguments.remapDeclToApiClasses(),
                throws = constructor.throws
            ) {
                addStatement(
                    Statement.Return(
                        Expression.Call.Constructor(
                            constructing = innerClass.innerMostClassNameAsType(),
                            parameters = apiPassedParameters(constructor),
                            receiver = Expression.This.castTo(classApi.asRawType())
                        ).castFromMcToApiClass(constructedInnerClass, doubleCast = doubleCastRequired(innerClass))
                    )
                )
            }
        }
    }

    private fun apiPassedParameters(method: ClassApi.Method): List<Pair<JvmType, Expression>> =
        method.parameters
            .map { (name, type) ->
                type.toJvmType() to Expression.Variable(name).castToMcClass(type, doubleCast = true)
            }


    private fun apiParametersDeclaration(method: ClassApi.Method) =
        method.parameters.mapValues { (_, v) -> v.remapToApiClass() }.toMap()

    private fun GeneratedClass.addArrayFactory() {
        addMethod(
            name = if (existsMethodWithSameDescriptorAsArrayFactory()) "${ArrayFactoryName}_factory" else ArrayFactoryName,
            visibility = Visibility.Public,
            parameters = mapOf(ArrayFactorySizeParamName to GenericsPrimitiveType.Int.noAnnotations()),
            returnType = ArrayGenericType(classApi.asType().type).noAnnotations().remapToApiClass()
                .pushAllArrayTypeArgumentsToInnermostClass(),
            abstract = false,
            final = false,
            static = true,
            typeArguments = allApiClassTypeArguments(),
            throws = listOf()
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

    private fun existsMethodWithSameDescriptorAsArrayFactory() = classApi.methods.any { method ->
        method.name == ArrayFactoryName && method.parameters.size == 1
                && method.parameters.values.first().type.let { it == GenericsPrimitiveType.Int }
    }

    internal fun allApiClassTypeArguments(): List<TypeArgumentDeclaration> = when {
        classApi.isStatic -> classApi.typeArguments.remapDeclToApiClasses()
        baseClass -> baseClassTypeArguments()
        else -> classApi.listInnerClassChain().flatMap { it.typeArguments.remapDeclToApiClasses() }
    }

    private fun baseClassTypeArguments(): List<TypeArgumentDeclaration> {
        return classApi.typeArguments.map { typeArg ->
            // For baseclasses, if a type is bounded by a mc type, we want to bound it in the api by both that mc type and the api version of it
            val classBound =
                if (typeArg.classBound?.isMcClass() == true) typeArg.classBound else typeArg.classBound?.remapToApiClass()
            val mcInterfaceBounds = typeArg.interfaceBounds.filter { it.isMcClass() }
            typeArg.copy(
                classBound = classBound,
                interfaceBounds = mcInterfaceBounds + typeArg.interfaceBounds.map { it.remapToApiClass() }
                    .applyIf(typeArg.classBound?.isMcClass() == true) { it + typeArg.classBound!!.remapToApiClass() }
//                    .appendIfNotNull()
            )
        }
    }

//    private fun GenericType.andRemappedToMcClass() = listOf(this, this.remapToApiClass())


    ////////// REMAPPING /////////

    private fun <T : GenericReturnType> JavaType<T>.remapToApiClass(): JavaType<T> =
        with(metadata.versionPackage) { remapToApiClass() }

    private fun <T : GenericReturnType> T.remapToApiClass(): T =
        with(metadata.versionPackage) { remapToApiClass() }

    private fun List<TypeArgumentDeclaration>.remapDeclToApiClasses(): List<TypeArgumentDeclaration> =
        with(metadata.versionPackage) { remapDeclToApiClasses() }

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

    //TODO: clean up the casting ordeal...

    private fun Expression.castFromMcToApiClass(
        type: AnyJavaType,
        doubleCast: Boolean? = null,
        forceCast: Boolean = false // In this case cast even when going from a mc class to an API class
    ): Expression =
        if (if (forceCast) type.castRequiredToMcClass() else type.castRequiredToApiClass()) {
            this.castTo(type.remapToApiClass(), doubleCast)
        } else this


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
private val NotNullAnnotation = JavaAnnotation(ObjectType("org/jetbrains/annotations/NotNull", dotQualified = false))
private val OverrideAnnotation = JavaAnnotation(ObjectType("java/lang/Override", dotQualified = false))