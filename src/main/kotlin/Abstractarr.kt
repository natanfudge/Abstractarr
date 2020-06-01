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
            abstractClass(classApi, destDir, metadata)
        }
//        }
    }
}
//TODO:
// - add version package
// arrays
// inner classes
// abstract classes
// interfaces
// enums
// baseclasses
// generics
// - superclasses
// annotations (nullable etc)

private fun abstractClass(api: ClassApi, destPath: Path, metadata: AbstractionMetadata) {
    check(api.visibility == ClassVisibility.Public)
    JavaCodeGenerator.writeClass(
        packageName = api.packageName.toApiPackageName(metadata), name = api.className.toApiClassName(),
        visibility = api.visibility, isAbstract = false, isInterface = true, writeTo = destPath
    ) { addClassBody(api, metadata) }

}

private fun String?.toApiPackageName(metadata: AbstractionMetadata) = "${metadata.versionPackage}.${this ?: ""}"
private fun String.toApiClassName() = "I$this"
//private fun String?.toMcPackageName(metadata: AbstractionMetadata) : String{
//    checkNotNull(this)
//    return removeSuffix(metadata.versionPackage + "/")
//}
//private fun String.toMcClass() = "I$this"

private fun <T : Descriptor>T.remapToApiClass(metadata: AbstractionMetadata) : T = remap {
    if (it.isMcClass()) {
        val (packageName, className) = it.splitFullyQualifiedName(dotQualified = false)
        "${packageName.toApiPackageName(metadata)}.${className.toApiClassName()}".replace(".", "/")
    } else it
}

private fun JavaGeneratedClass.addClassBody(api: ClassApi, metadata: AbstractionMetadata) {
    val mcClassType = api.nameAsType()
    for (method in api.methods) {
        if (!method.isPublic) continue
        val parameters = method.parameters.mapValues { (_, v) -> v.remapToApiClass(metadata) }.toList()

        val returnType = (if(method.isConstructor) api.nameAsType() else method.returnType).remapToApiClass(metadata)

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
                //TODO: generics is gonna be annoying, raw types for the rescue?
                if (type.isMcClass()) variable.castTo(type/*.remapToApiClass(metadata)*/) else variable
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


            addStatement(if (!method.isVoid || method.isConstructor) {
                check(returnType is AnyType) // because it's not void
                val returnedValue = if(returnType.isApiClass(metadata)) call.castTo(returnType) else call
                Statement.Return(returnedValue)
            } else call)
        }


    }
}


private fun String.isMcClass(): Boolean = startsWith("net/minecraft/")
private fun Descriptor.isMcClass(): Boolean = this is ObjectType && className.isMcClass()
private fun String.isApiClass(metadata : AbstractionMetadata): Boolean = startsWith("${metadata.versionPackage}/net/minecraft/")
private fun Descriptor.isApiClass(metadata : AbstractionMetadata): Boolean = this is ObjectType && className.isApiClass(metadata)