import api.*
import codegeneration.*
import descriptor.Descriptor
import descriptor.PrimitiveType
import descriptor.remap
import signature.*
import java.nio.file.Path

data class AbstractionMetadata(val versionPackage: String, val classPath: List<Path>)

//TODO:
// deal with mc classes that extend non minecraft classes
// apply interfaces only at runtime
// don't provide api for overriden methods (that should be exposed through interface extension)
// enums
// baseclasses
// - superclasses
// annotations (nullable etc)
// arrays
// generics (use same generics in methods and fields and supertypes)
// wildcards
// think about what happens when there is anon classes or lambdas
// @NoNull on all .create() functions
// throws

object Abstractor {
    fun abstract(mcJar: Path, destDir: Path, metadata: AbstractionMetadata) {
        require(destDir.parent.exists()) { "The chosen destination path '$destDir' is not in any existing directory." }
        require(destDir.parent.isDirectory()) { "The parent of the chosen destination path '$destDir' is not a directory." }

        destDir.deleteRecursively()
        destDir.createDirectory()

        val index = indexClasspath(metadata.classPath + listOf(mcJar) /*+ getJvmBootClasses()*/)

        for (classApi in ClassApi.readFromJar(mcJar)) {
            if (!classApi.isPublicApi) continue
            ClassAbstractor(metadata, index, classApi).abstractClass(destDir, outerClass = null)
        }
    }
}

//private fun superTyped()
//private val SuperTypedClass = ObjectType("febb/apiruntime/SuperTyped", dotQualified = false)
private val SuperTypedPackage = PackageName(listOf("febb", "apiruntime"))
private const val SuperTypedName = "SuperTyped"

private fun <T> List<T>.appendIfNotNull(value: T?) = if (value == null) this else this + value

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

        // No need to add I for inner classes
        val className = if (outerClass == null) classApi.name.toApiClass() else classApi.name
        val visibility = classApi.visibility
        val isAbstract = false
        val isInterface = true

        // Add SuperTyped<SuperClass> superinterface for classes which have a non-mc superclass
        val superTypedInterface = classApi.superClass?.let {
            if (it.containsMcClasses()) it.remapToApiClass() else JavaClassType(
                type = ClassGenericType(
                    SuperTypedPackage,
                    listOf(
                        SimpleClassGenericType(
                            SuperTypedName,
                            listOf(TypeArgument.SpecificType(it.type, wildcardType = null))
                        )
                    )
                ),
                annotations = listOf()
            )
        }
        val superInterfaces = classApi.superInterfaces.map { it.remapToApiClass() }.appendIfNotNull(superTypedInterface)

        if (destPath != null) {
            JavaCodeGenerator.writeClass(
                name = className,
                visibility = visibility, isAbstract = isAbstract, isInterface = isInterface, writeTo = destPath,
                superClass = null, superInterfaces = superInterfaces
            ) { addClassBody() }
        } else {
            requireNotNull(outerClass)
            outerClass.addInnerClass(
                name = className.shortName.innermostClass(),
                visibility = visibility, isAbstract = isAbstract, isInterface = isInterface, isStatic = true,
                superClass = null, superInterfaces = superInterfaces
            ) { addClassBody() }
        }
    }

    private fun PackageName?.toApiPackageName() = metadata.versionPackage.prependToQualified(this ?: PackageName.Empty)
    private fun ShortClassName.toApiShortClassName() = ShortClassName(("I" + outerClass()).prependTo(innerClasses()))

    private fun QualifiedName.toApiClass(): QualifiedName = if (isMcClassName()) {
        QualifiedName(packageName = packageName.toApiPackageName(), shortName = shortName.toApiShortClassName())
    } else this

    private fun <T : GenericReturnType> JavaType<T>.remapToApiClass(): JavaType<T> = remap { it.toApiClass() }
    private fun <T : Descriptor> T.remapToApiClass(): T = remap { it.toApiClass() }

    private fun JavaGeneratedClass.addClassBody() {
        for (method in classApi.methods) {
            abstractMethod(method)
        }

        for (field in classApi.fields) {
            if (!field.isPublic) continue
            if (field.isFinal && field.isStatic) {
                addConstant(field)
            } else {
                addGetter(field)
            }

            if (!field.isFinal) {
                addSetter(field)
            }
        }

        for (innerClass in classApi.innerClasses) {
            if (!innerClass.isPublic) continue
            ClassAbstractor(metadata, index, innerClass).abstractClass(destPath = null, outerClass = this)

            // Inner classes are constructed by their parent
            if (!innerClass.isStatic) {
                addInnerClassConstructor(innerClass)
            }
        }
    }

    private fun JavaGeneratedClass.addInnerClassConstructor(innerClass: ClassApi) {
        for (constructor in innerClass.methods.filter { it.isConstructor }) {
            val constructedInnerClass = innerClass.nameAsType()
            addMethod(
                name = "new" + innerClass.name.shortName.innermostClass(),
                visibility = Visibility.Public,
                static = false,
                final = false,
                abstract = false,
                returnType = constructedInnerClass.remapToApiClass(),
                parameters = apiParametersDeclaration(constructor)
                    //no need for the this$0 outer class reference
                    .toList().drop(1).toMap()
            ) {
                addStatement(
                    Statement.Return(
                        Expression.Call.Constructor(
                            constructing = innerClass.innerMostClassNameAsType(),
                            parameters = apiPassedParameters(constructor)
                                //no need for the this$0 outer class reference
                                .drop(1),
                            receiver = Expression.This.castTo(classApi.nameAsType())
                        ).castFromMcToApiClass(constructedInnerClass, doubleCast = doubleCastRequired(innerClass))
                    )
                )
            }
        }
    }

    private fun JavaGeneratedClass.addConstant(field: ClassApi.Field) {
        addField(
            name = field.name, final = true, static = true, visibility = Visibility.Public,
            type = field.type.remapToApiClass(),
            initializer = abstractedFieldExpression(field).castFromMcToApiClass(field.type)
        )
    }

    private fun JavaGeneratedClass.addSetter(field: ClassApi.Field) {
        addMethod(
            name = "set" + field.name.capitalize(),
            parameters = mapOf(field.name to field.type.remapToApiClass()),
            visibility = Visibility.Public,
            returnType = GenericReturnType.Void.noAnnotations(),
            abstract = false,
            static = field.isStatic,
            final = false
        ) {
            addStatement(
                Statement.Assignment(
                    target = abstractedFieldExpression(field),
                    assignedValue = Expression.Variable(field.name).castFromMcClass(field.type)
                )
            )
        }
    }

    private fun JavaGeneratedClass.addGetter(field: ClassApi.Field) {
        val getterName = field.getGetterPrefix() +
                // When it starts with "is" no prefix is added so there's no need to capitalize
                if (field.name.startsWith("is")) field.name else field.name.capitalize()
        addMethod(
            // Add _field when getter clashes with a method of the same name
            name = if (classApi.methods.any { it.parameters.isEmpty() && it.name == getterName }) getterName + "_field"
            else getterName,
            parameters = mapOf(),
            visibility = Visibility.Public,
            returnType = field.type.remapToApiClass(),
            abstract = false,
            static = field.isStatic,
            final = false
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
        if (type.type.let { it is GenericsPrimitiveType && it.primitive == PrimitiveType.Boolean }) {
            if (name.startsWith("is")) "" else "is"
        } else "get"


    private fun abstractedFieldExpression(
        field: ClassApi.Field
    ): Expression.Field {
        val mcClassType = classApi.nameAsType()
        return Expression.Field(
            owner = if (field.isStatic) ClassReceiver(mcClassType) else Expression.This.castTo(mcClassType),
            name = field.name
        )
    }


    private fun JavaGeneratedClass.abstractMethod(method: ClassApi.Method) {
        // Only public methods are abstracted
        if (!method.isPublic) return
        // Abstract classes/interfaces can't be constructed directly
        if (method.isConstructor && (classApi.isInterface || classApi.isAbstract)) return
        // Inner classes need to be constructed by their parent class
        if (classApi.isInnerClass && method.isConstructor && !classApi.isStatic) return

        // Don't duplicate methods that are just being overriden
        if (method.isOverride()) return

        val parameters = apiParametersDeclaration(method)

        val mcReturnType = if (method.isConstructor) classApi.nameAsType() else method.returnType
        val returnType = mcReturnType.remapToApiClass()

        val mcClassType = classApi.nameAsType()

        addMethod(
            name = if (method.isConstructor) "create" else method.name,
            visibility = method.visibility, parameters = parameters,
            final = false,
            static = method.isStatic || method.isConstructor,
            abstract = false,
            returnType = returnType
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
                    check(returnType.type is GenericTypeOrPrimitive) // because it's not void
                    @Suppress("UNCHECKED_CAST")
                    val returnedValue = call.castFromApiClassTo(returnType as JavaType<GenericTypeOrPrimitive>)
                    Statement.Return(returnedValue)
                } else call
            )
        }
    }


    private fun ClassApi.Method.isOverride() = !isConstructor && !isStatic &&
            index.getSuperTypesRecursively(classApi.name)
                .any { index.classHasMethod(it, name, getJvmDescriptor()) }

    // the finalness of parameters individually
    private fun apiPassedParameters(method: ClassApi.Method): List<Expression> =
        method.parameters
            .map { (name, type) -> Expression.Variable(name).castFromMcClass(type, doubleCast = true) }

    /**
     * For any type Child, and type Super, such that Super is a supertype of Child:
     * Casting from Child[] to Super[] is valid,
     * Casting from Super[] to Child[] will always fail.
     */

    private fun Expression.castFromMcToApiClass(type: AnyJavaType, doubleCast: Boolean? = null): Expression =
        if (type.containsMcClasses()) this.castTo(type.remapToApiClass(), doubleCast) else this


    private fun Expression.castFromMcClass(type: AnyJavaType, doubleCast: Boolean? = null): Expression =
        if (type.containsMcClasses()) castTo(type, doubleCast) else this


    private fun Expression.castFromApiClassTo(type: AnyJavaType): Expression =
        if (type.containsApiClasses()) this.castTo(type) else this

    private fun apiParametersDeclaration(method: ClassApi.Method) =
        method.parameters.mapValues { (_, v) -> v.remapToApiClass() }.toMap()


    private fun PackageName?.isMcPackage(): Boolean = this?.startsWith("net", "minecraft") == true
    private fun QualifiedName.isMcClassName(): Boolean = packageName.isMcPackage()
    private fun PackageName?.isApiClass(): Boolean =
        this?.startsWith(metadata.versionPackage, "net", "minecraft") == true

//    private fun QualifiedName.isApiClass(): Boolean = packageName.isApiClass()

    //    private fun Descriptor.isMcClass(): Boolean = this is ObjectType && fullClassName.isMcClass()
    private fun JavaType<*>.containsMcClasses(): Boolean = type.getContainedClassesRecursively()
        .any { it.packageName.isMcPackage() }

    private fun AnyJavaType.containsApiClasses(): Boolean = type.getContainedClassesRecursively()
        .any { it.packageName.isApiClass() }
    private fun doubleCastRequired(@Suppress("UNUSED_PARAMETER") classApi: ClassApi)
            = true /*classApi.isFinal*/ // the rules seem too ambiguous

    private fun Expression.castTo(type: AnyJavaType, forceDoubleCast: Boolean? = null): Expression =
        castExpressionTo(type, forceDoubleCast ?: doubleCastRequired(classApi))
}