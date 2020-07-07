package abstractor

import api.*
import codegeneration.*
import metautils.codegeneration.asm.AsmCodeGenerator
import kotlinx.serialization.Serializable
import metautils.codegeneration.asm.toAsmAccess
import metautils.api.*
import metautils.codegeneration.*
import metautils.descriptor.JvmPrimitiveType
import metautils.descriptor.JvmType
import metautils.descriptor.ObjectType
import metautils.descriptor.ReturnDescriptor
import metautils.signature.*
import metautils.util.*
import metautils.signature.annotated
import metautils.signature.noAnnotations
import metautils.signature.toJvmQualifiedName
import metautils.signature.toJvmType
import metautils.util.createDirectory
import metautils.util.deleteRecursively
import metautils.util.exists
import metautils.util.isDirectory
import java.nio.file.Path


data class AbstractionMetadata(
    val versionPackage: VersionPackage,
    val classPath: List<Path>,
    val fitToPublicApi: Boolean,
    val writeRawAsm: Boolean,
    val createBaseClassesFor: (ClassApi) -> Boolean,
    val javadocs: JavaDocs
)
/** A list used in testing and production to know what interfaces/classes to attach to minecraft interface */
typealias AbstractionManifest = Map<String, AbstractedClassInfo>

@Serializable
data class AbstractedClassInfo(val apiClassName: String, /*val isThrowable: Boolean,*/ val newSignature: String)

class Abstractor private constructor(
    private val classes: Collection<ClassApi>,
    private val classNamesToClasses: Map<QualifiedName, ClassApi>,
    private val index: ClasspathIndex
) {

    companion object {
        fun parse(mcJar: Path, metadata: AbstractionMetadata, usage: (Abstractor) -> Unit): AbstractionManifest {
            val classes = ClassApi.readFromJar(mcJar) { path ->
                path.toString().let { it.startsWith("/net/minecraft/") || it.startsWith("/com/mojang/blaze3d") }
            }
            val classNamesToClasses = classes.flatMap { outerClass ->
                outerClass.allInnerClassesAndThis().map { it.name to it }
            }.toMap()

            // We need to add the access of api interfaces, base classes, and base api interfaces.
            // For other things in ClassEntry we just pass empty list in assumption they won't be needed.
            val additionalEntries = listAllGeneratedClasses(classes, metadata)

            return ClasspathIndex.index(metadata.classPath + listOf(mcJar), additionalEntries) {
                usage(Abstractor(classes, classNamesToClasses, it))
                buildAbstractionManifest(classes, metadata.versionPackage)
            }
        }
    }

    fun abstract(destDir: Path, metadata: AbstractionMetadata) {
        require(destDir.parent.exists()) { "The chosen destination path '$destDir' is not in any existing directory." }
        require(destDir.parent.isDirectory()) { "The parent of the chosen destination path '$destDir' is not a directory." }

        destDir.deleteRecursively()
        destDir.createDirectory()

        for (classApi in classes) {
            if (!classApi.isPublicApi) continue
            ClassAbstractor(metadata, index, classApi, classNamesToClasses)
                .abstractClass(destPath = destDir)
        }
    }
}


private fun buildAbstractionManifest(
    classes: Collection<ClassApi>,
    version: VersionPackage
): AbstractionManifest = with(version) {
    classes.flatMap { outerClass ->
        outerClass.allInnerClassesAndThis().map { mcClass ->
            val mcClassName = mcClass.name.toSlashQualifiedString()
            val apiClass = mcClass.name.toApiClass()
            val oldSignature = mcClass.getSignature()
            val insertedApiClass = ClassGenericType.fromNameAndTypeArgs(
                name = apiClass,
                typeArgs = allApiInterfaceTypeArguments(mcClass).toTypeArgumentsOfNames()
            )

            val newSignature = oldSignature.copy(superInterfaces = oldSignature.superInterfaces + insertedApiClass)
            mcClassName to AbstractedClassInfo(
                apiClassName = apiClass.toSlashQualifiedString(),
                newSignature = newSignature.toClassfileName()
            )
        }
    }.toMap()
}

private fun listAllGeneratedClasses(
    classes: Collection<ClassApi>,
    metadata: AbstractionMetadata
): Map<QualifiedName, ClassEntry> = with(metadata.versionPackage) {
    classes.flatMap { outerClass ->
        // This also includes package private and private stuff, because mojang sometimes exposes private classes
        // in public apis... thank you java
        outerClass.allInnerClassesAndThis().flatMap { mcClass ->
            val baseclass = mcClass.name.toBaseClass() to entryJustForAccess(
                baseClassAccess(origIsInterface = mcClass.isInterface),
                isStatic = mcClass.isStatic,
                visibility = mcClass.visibility
            )

            val apiInterface = mcClass.name.toApiClass() to entryJustForAccess(
                apiInterfaceAccess(metadata),
                isStatic = mcClass.isInnerClass,
                visibility = Visibility.Public
            )

            listOf(apiInterface, baseclass)
        }
    }.toMap()
}

private fun entryJustForAccess(access: ClassAccess, visibility: Visibility, isStatic: Boolean): ClassEntry {
    return ClassEntry(
        methods = mapOf(), superInterfaces = listOf(), superClass = null,
        access = access.toAsmAccess(visibility, isStatic),
        name = QualifiedName.Empty
    )
}


internal fun apiInterfaceAccess(metadata: AbstractionMetadata) = ClassAccess(
    isFinal = metadata.fitToPublicApi,
    variant = ClassVariant.Interface
)


internal fun baseClassAccess(origIsInterface: Boolean) = ClassAccess(
    isFinal = false,
    variant = if (origIsInterface) ClassVariant.Interface else ClassVariant.AbstractClass
)

