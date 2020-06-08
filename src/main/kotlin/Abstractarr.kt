import api.*
import codegeneration.*
import descriptor.*
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
// generics
// wildcards
// think about what happens when there is anon classes or lambdas


object Abstractor {
    fun abstract(mcJar: Path, destDir: Path, metadata: AbstractionMetadata) {
        require(destDir.parent.exists()) { "The chosen destination path '$destDir' is not in any existing directory." }
        require(destDir.parent.isDirectory()) { "The parent of the chosen destination path '$destDir' is not a directory." }

        destDir.deleteRecursively()
        destDir.createDirectory()

        val index = indexClasspath(metadata.classPath + listOf(mcJar) + getJvmBootClasses())

        for (classApi in ClassApi.readFromJar(mcJar)) {
            if (!classApi.isPublicApi) continue
//                val destPath = destFs.getPath(/*classApi.fullyQualifiedName.replace(".", "/")*/ "/")
            ClassAbstractor(metadata, index, classApi).abstractClass(destDir, outerClass = null)
        }
//        }
    }
}

private val SuperTyped = ObjectType("febb/apiruntime/SuperTyped")

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
        val className = if (outerClass == null) classApi.className.toApiClassName() else classApi.className
        val visibility = classApi.visibility
        val isAbstract = false
        val isInterface = true

        val superTypedInterface = classApi.superClass?.let {
            SuperType(
                rawType = SuperTyped, annotations = listOf(),
                typeArguments = listOf(it)
            )
        }
        val superInterfaces = classApi.superInterfaces.map { it.remapToApiClass() }.appendIfNotNull(superTypedInterface)

        if (destPath != null) {
            JavaCodeGenerator.writeClass(
                packageName = classApi.packageName.toApiPackageName(), name = className,
                visibility = visibility, isAbstract = isAbstract, isInterface = isInterface, writeTo = destPath,
                superClass = null, superInterfaces = superInterfaces
            ) { addClassBody() }
        } else {
            requireNotNull(outerClass)
            outerClass.addInnerClass(
                name = className,
                visibility = visibility, isAbstract = isAbstract, isInterface = isInterface, isStatic = true,
                superClass = null, superInterfaces = superInterfaces
            ) { addClassBody() }
        }
    }

    private fun String?.toApiPackageName() = "${metadata.versionPackage}.${this ?: ""}"
    private fun String.toApiClassName() = "I$this"

    private fun String.toApiClass() = if (isMcClass()) {
        val (packageName, className) = splitFullyQualifiedName(dotQualified = false)
        "${packageName.toApiPackageName()}.${className.toApiClassName()}".replace(".", "/")
    } else this

    private fun <T : GenericJavaType> T.remapToApiClass(): T = remap { it.toApiClass() }
    private fun <T : Descriptor> T.remapToApiClass(): T = remap { it.toApiClass() }

    private fun JavaGeneratedClass.addClassBody() {
        val mcClassType = classApi.nameAsType()
        for (method in classApi.methods) {
            abstractMethod(method, mcClassType)
        }

        for (field in classApi.fields) {
            if (!field.isPublic) continue
            if (field.isFinal && field.isStatic) {
                addConstant(field, mcClassType)
            } else {
                addGetter(field, mcClassType)
            }

            if (!field.isFinal) {
                addSetter(field, mcClassType)
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
                name = "new" + innerClass.className,
                visibility = Visibility.Public,
                static = false,
                final = false,
                abstract = false,
                returnType = constructedInnerClass.remapToApiClass(),
                parameters = apiParametersDeclaration(constructor)
            ) {
                addStatement(
                    Statement.Return(
                        Expression.Call.Constructor(
                            constructing = innerClass.innerClassNameAsType(),
                            parameters = apiPassedParameters(constructor),
                            receiver = Expression.This.castTo(classApi.nameAsType())
                        ).castFromMcToApiClass(constructedInnerClass, doubleCast = doubleCastRequired(innerClass))
                    )
                )
            }
        }
    }

    private fun JavaGeneratedClass.addConstant(
        field: ClassApi.Field,
        mcClassType: ObjectType
    ) {
        addField(
            name = field.name, final = true, static = true, visibility = Visibility.Public,
            type = field.descriptor.remapToApiClass(),
            initializer = abstractedFieldExpression(field, mcClassType).castFromMcToApiClass(field.descriptor)
        )
    }

    private fun JavaGeneratedClass.addSetter(
        field: ClassApi.Field,
        mcClassType: ObjectType
    ) {
        addMethod(
            name = "set" + field.name.capitalize(),
            parameters = mapOf(field.name to field.descriptor.remapToApiClass()),
            visibility = Visibility.Public,
            returnType = ReturnDescriptor.Void,
            abstract = false,
            static = field.isStatic,
            final = false
        ) {
            addStatement(
                Statement.Assignment(
                    target = abstractedFieldExpression(field, mcClassType),
                    assignedValue = Expression.Variable(field.name).castFromMcClass(field.descriptor)
                )
            )
        }
    }

    private fun JavaGeneratedClass.addGetter(
        field: ClassApi.Field,
        mcClassType: ObjectType
    ) {
        addMethod(
            name = "get" + field.name.capitalize(),
            parameters = mapOf(),
            visibility = Visibility.Public,
            returnType = field.descriptor.remapToApiClass(),
            abstract = false,
            static = field.isStatic,
            final = false
        ) {
            val fieldAccess = abstractedFieldExpression(field, mcClassType)

            addStatement(
                Statement.Return(
                    fieldAccess.castFromMcToApiClass(field.descriptor)
                )
            )
        }
    }


    private fun abstractedFieldExpression(
        field: ClassApi.Field,
        mcClassType: ObjectType
    ): Expression.Field {
        return Expression.Field(
            owner = if (field.isStatic) ClassReceiver(mcClassType) else Expression.This.castTo(mcClassType),
            name = field.name
        )
    }


    private fun JavaGeneratedClass.abstractMethod(
        method: ClassApi.Method,
        mcClassType: ObjectType
    ) {
        // Only public methods are abstracted
        if (!method.isPublic) return
        // Abstract classes/interfaces can't be constructed directly
        if (method.isConstructor && (classApi.isInterface || classApi.isAbstract)) return
        // Inner classes need to be constructed by their parent class
        if (classApi.isInnerClass && method.isConstructor && !classApi.isStatic) return

        // Don't duplicate methods that are just being overriden
        if (method.isOverride()) return

        val parameters = apiParametersDeclaration(method)

        val returnType = (if (method.isConstructor) classApi.nameAsType() else method.returnType).remapToApiClass()

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
                    check(returnType is JvmType) // because it's not void
                    val returnedValue = call.castFromApiClass(returnType)
                    Statement.Return(returnedValue)
                } else call
            )
        }
    }

    //TODO: this won't work when the overriding method uses parameters or return types which are subclasses of those in the parent method.
    // (for example: parent accepts List, overriding child accepts ArrayList)
    private fun ClassApi.Method.isOverride() =
        index.getSuperTypesRecursively(classApi.fullInnerName().replace('.', '/'))
            .any { index.classHasMethod(it, name, descriptor) }

    // the finalness of parameters individually
    private fun apiPassedParameters(method: ClassApi.Method): List<Expression> =
        method.parameters.map { (name, type) -> Expression.Variable(name).castFromMcClass(type, doubleCast = true) }

    /**
     * For any type Child, and type Super, such that Super is a supertype of Child:
     * Casting from Child[] to Super[] is valid,
     * Casting from Super[] to Child[] will always fail.
     */

    private fun Expression.castFromMcToApiClass(type: JvmType, doubleCast: Boolean? = null): Expression =
        if (type.isMcClass()) this.castTo(type.remapToApiClass(), doubleCast) else this


    private fun Expression.castFromMcClass(type: JvmType, doubleCast: Boolean? = null): Expression =
        if (type.isMcClass()) castTo(type, doubleCast) else this


    private fun Expression.castFromApiClass(type: JvmType): Expression =
        if (type.isApiClass()) this.castTo(type) else this

    private fun apiParametersDeclaration(method: ClassApi.Method) =
        method.parameters.mapValues { (_, v) -> v.remapToApiClass() }.toMap()


    private fun String.isMcClass(): Boolean = startsWith("net/minecraft/")
    private fun Descriptor.isMcClass(): Boolean = this is ObjectType && className.isMcClass()
    private fun String.isApiClass(): Boolean =
        startsWith("${metadata.versionPackage}/net/minecraft/")

    private fun Descriptor.isApiClass(): Boolean =
        this is ObjectType && className.isApiClass()

    private fun doubleCastRequired(classApi: ClassApi) = classApi.isFinal

    private fun Expression.castTo(type: JvmType, forceDoubleCast: Boolean? = null): Expression =
        castExpressionTo(type, forceDoubleCast ?: doubleCastRequired(classApi))
}