import api.*
import codegeneration.*
import descriptor.remap
import java.nio.file.Path

data class AbstractionMetadata(val versionPackage: String)

object Abstractor {
    fun abstract(mcJar: Path, destJar: Path, metadata : AbstractionMetadata) {
        require(destJar.parent.exists()) { "The chosen destination path '$destJar' is not in any existing directory." }
        require(destJar.parent.isDirectory()) { "The parent of the chosen destination path '$destJar' is not a directory." }

        destJar.deleteIfExists()
        destJar.createJar()

        destJar.openJar { destFs ->
            for (classApi in ClassApi.readFromJar(mcJar)) {
                if (!classApi.isPublicApi) continue
                val destPath = destFs.getPath(/*classApi.fullyQualifiedName.replace(".", "/")*/ "/")
                abstractClass(classApi, destPath, metadata)
            }
        }
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

private fun abstractClass(api: ClassApi, destPath: Path, metadata: AbstractionMetadata) {
    check(api.visibility == ClassVisibility.Public)
    JavaCodeGenerator.writeClass(
        packageName = api.packageName.toApiPackageName(metadata), name = api.className.toApiClassName(),
        visibility = api.visibility, isAbstract = false, isInterface = true, writeTo = destPath
    ) { addClassBody(api, metadata) }

}

private fun String?.toApiPackageName(metadata: AbstractionMetadata) =  "${metadata.versionPackage}.${this ?: ""}"
private fun String.toApiClassName() =  "I$this"

private fun JavaGeneratedClass.addClassBody(api: ClassApi,metadata: AbstractionMetadata) {
    val classType = api.nameAsType()
    for (method in api.methods) {
        if (!method.isPublic) continue
        val parameters = method.parameters.mapValues { (_, v) ->
            v.remap {
                if (it.isMcClass()) {
                    val (packageName, className) = it.splitFullyQualifiedName()
                    "${packageName.toApiPackageName(metadata)}.${className.toApiClassName()}"
                } else it
            }
        }.toList()

        val body: JavaGeneratedMethod.() -> Unit = {
            addFunctionCall(
                receiver = Expression.Value.This.castTo(classType),
                returnResult = method.isVoid,
                parameters = method.parameterNames.map { Expression.Value.Variable(it) },
                methodName = method.name
            )
        }
        if (method.isConstructor) {
            //TODO: constructors
        } else {
            addMethod(
                name = method.name, //TODO: constructors
                visibility = method.visibility, parameters = parameters,
                final = false,
                static = false,//TODO: constructors
                abstract = false,
                returnType = method.returnType,
                body = body
            )
        }

    }
}

private fun String.isMcClass(): Boolean = startsWith("net.minecraft")