import api.*
import codegeneration.*
import descriptor.*
import java.nio.file.Path

data class AbstractionMetadata(val versionPackage: String)

object Abstractor {
    fun abstract(mcJar: Path, destDir: Path, metadata: AbstractionMetadata) {
        require(destDir.parent.exists()) { "The chosen destination path '$destDir' is not in any existing directory." }
        require(destDir.parent.isDirectory()) { "The parent of the chosen destination path '$destDir' is not a directory." }

        destDir.deleteRecursively()
        destDir.createDirectory()
//        destDir.createJar()

//        destDir.openJar { destFs ->
        for (classApi in ClassApi.readFromJar(mcJar)) {
            if (!classApi.isPublicApi) continue
//                val destPath = destFs.getPath(/*classApi.fullyQualifiedName.replace(".", "/")*/ "/")
            AbstractorImpl(metadata).abstractClass(classApi, destDir, outerClass = null)
        }
//        }
    }
}

private class AbstractorImpl(private val metadata: AbstractionMetadata) {
    /**
     * destPath == null and outerClass != null when it's an inner class
     */
    fun abstractClass(api: ClassApi, destPath: Path?, outerClass: JavaGeneratedClass?) {
        check(api.visibility == ClassVisibility.Public)

        // No need to add I for inner classes
        val className = if (outerClass == null) api.className.toApiClassName() else api.className
        val visibility = api.visibility
        val isAbstract = false
        val isInterface = true

        if (destPath != null) {
            JavaCodeGenerator.writeClass(
                packageName = api.packageName.toApiPackageName(), name = className,
                visibility = visibility, isAbstract = isAbstract, isInterface = isInterface, writeTo = destPath
            ) { addClassBody(api) }
        } else {
            requireNotNull(outerClass)
            outerClass.addInnerClass(
                name = className,
                visibility = visibility,
                isAbstract = isAbstract,
                isInterface = isInterface,
                isStatic = true
            ) { addClassBody(api) }
        }
    }

    private fun String?.toApiPackageName() = "${metadata.versionPackage}.${this ?: ""}"
    private fun String.toApiClassName() = "I$this"


    private fun <T : Descriptor> T.remapToApiClass(): T = remap {
        if (it.isMcClass()) {
            val (packageName, className) = it.splitFullyQualifiedName(dotQualified = false)
            "${packageName.toApiPackageName()}.${className.toApiClassName()}".replace(".", "/")
        } else it
    }

    private fun JavaGeneratedClass.addClassBody(api: ClassApi) {
        val mcClassType = api.nameAsType()
        for (method in api.methods) {
            abstractMethod(method, api, mcClassType)
        }

        for (field in api.fields) {
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

        for (innerClass in api.innerClasses) {
            if (!innerClass.isPublic) continue
            abstractClass(innerClass, destPath = null, outerClass = this)

            // Inner classes are constructed by their parent
            if (!innerClass.isStatic) {
                for (constructor in innerClass.methods.filter { it.isConstructor }) {
                    val constructedInnerClass = innerClass.nameAsType().remapToApiClass()
                    addMethod(
                        name = "new" + innerClass.className,
                        visibility = Visibility.Public,
                        static = false,
                        final = false,
                        abstract = false,
                        returnType = constructedInnerClass,
                        parameters = apiParametersDeclaration(constructor)
                    ) {
                        addStatement(
                            Statement.Return(
                                Expression.Call.Constructor(
                                    constructing = constructedInnerClass,
                                    parameters = apiPassedParameters(constructor)
                                ).castFromApiClass(constructedInnerClass)
                            )
                        )
                    }
                }
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

            addStatement(Statement.Return(fieldAccess.castFromMcToApiClass(field.descriptor)))
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
        api: ClassApi,
        mcClassType: ObjectType
    ) {
        // Only public methods are abstracted
        if (!method.isPublic) return
        // Abstract classes/interfaces can't be constructed directly
        if (method.isConstructor && (api.isInterface || api.isAbstract)) return
        // Inner classes need to be constructed by their parent class
        if (method.isConstructor && !api.isStatic) return

        val parameters = apiParametersDeclaration(method)

        val returnType = (if (method.isConstructor) api.nameAsType() else method.returnType).remapToApiClass()

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
                    parameters = passedParameters
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
                    check(returnType is AnyType) // because it's not void
                    val returnedValue = call.castFromApiClass(returnType)
                    Statement.Return(returnedValue)
                } else call
            )
        }
    }

    private fun apiPassedParameters(method: ClassApi.Method) =
        method.parameters.map { (name, type) -> Expression.Variable(name).castFromMcClass(type) }


    private fun Expression.castFromMcToApiClass(type: AnyType): Expression =
        if (type.isMcClass()) this.castTo(type.remapToApiClass()) else this


    private fun Expression.castFromMcClass(type: AnyType): Expression =
        if (type.isMcClass()) this.castTo(type) else this


    private fun Expression.castFromApiClass(type: AnyType): Expression =
        if (type.isApiClass()) this.castTo(type) else this

    private fun apiParametersDeclaration(method: ClassApi.Method) =
        method.parameters.mapValues { (_, v) -> v.remapToApiClass() }.toMap()


    private fun String.isMcClass(): Boolean = startsWith("net/minecraft/")
    private fun Descriptor.isMcClass(): Boolean = this is ObjectType && className.isMcClass()
    private fun String.isApiClass(): Boolean =
        startsWith("${metadata.versionPackage}/net/minecraft/")

    private fun Descriptor.isApiClass(): Boolean =
        this is ObjectType && className.isApiClass()
}

//TODO:
// non-static inner classes
// abstract classes
// interfaces
// enums
// baseclasses
// - superclasses
// annotations (nullable etc)
// arrays
// generics
// think about what happens when there is anon classes or lambdas
