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
            AbstractorImpl(metadata).abstractClass(classApi, destDir)
        }
//        }
    }
}

private class AbstractorImpl(private val metadata: AbstractionMetadata) {
    fun abstractClass(api: ClassApi, destPath: Path) {
        check(api.visibility == ClassVisibility.Public)
        JavaCodeGenerator.writeClass(
            packageName = api.packageName.toApiPackageName(), name = api.className.toApiClassName(),
            visibility = api.visibility, isAbstract = false, isInterface = true, writeTo = destPath
        ) { addClassBody(api) }

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
    }

    private fun JavaGeneratedClass.addConstant(
        field: ClassApi.Field,
        mcClassType: ObjectType
    ) {
        addField(
            name = field.name, final = true, static = true, visibility = Visibility.Public,
            type = field.descriptor.remapToApiClass(),
            initializer = abstractedFieldExpression(field, mcClassType).castToApiClass(field.descriptor)
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
            addStatement(Statement.Assignment(
                target = abstractedFieldExpression(field, mcClassType),
                assignedValue = Expression.Variable(field.name).castIfNeededTo(field.descriptor)
            ))
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

            addStatement(Statement.Return(fieldAccess.castToApiClass(field.descriptor)))
        }
    }

    private fun Expression.castToApiClass(type: AnyType): Expression =
        if (type.isMcClass()) this.castTo(type.remapToApiClass()) else this


    private fun Expression.castIfNeededTo(type: AnyType): Expression =
        if (type.isMcClass()) this.castTo(type) else this

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
        if (!method.isPublic) return
        if (method.isConstructor && (api.isInterface || api.isAbstract)) return

        val parameters = method.parameters.mapValues { (_, v) -> v.remapToApiClass() }.toMap()

        val returnType = (if (method.isConstructor) api.nameAsType() else method.returnType).remapToApiClass()

        addMethod(
            name = if (method.isConstructor) "create" else method.name,
            visibility = method.visibility, parameters = parameters,
            final = false,
            static = method.isStatic || method.isConstructor,
            abstract = false,
            returnType = returnType
        ) {
            val passedParameters = method.parameters.map { (name, type) ->
                val variable = Expression.Variable(name)
                if (type.isMcClass()) variable.castTo(type) else variable
            }
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
                    val returnedValue = if (returnType.isApiClass()) call.castTo(returnType) else call
                    Statement.Return(returnedValue)
                } else call
            )
        }
    }


    private fun String.isMcClass(): Boolean = startsWith("net/minecraft/")
    private fun Descriptor.isMcClass(): Boolean = this is ObjectType && className.isMcClass()
    private fun String.isApiClass(): Boolean =
        startsWith("${metadata.versionPackage}/net/minecraft/")

    private fun Descriptor.isApiClass(): Boolean =
        this is ObjectType && className.isApiClass()
}

//TODO:
// inner classes
// abstract classes
// interfaces
// enums
// baseclasses
// - superclasses
// annotations (nullable etc)
// arrays
// generics

