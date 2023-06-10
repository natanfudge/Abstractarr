package abstractor

import metautils.api.JavaType
import metautils.signature.*
import metautils.types.ArrayType
import metautils.types.JvmType
import metautils.types.ObjectType
import metautils.util.*

class VersionPackage internal constructor(private val versionPackage: String) {
    companion object {
        fun fromMcVersion(mcVersion: String) = VersionPackage("v" + mcVersion.replace(".", "_"))
    }

    private fun PackageName.toApiPackageName() = versionPackage.prependToQualified(this)
    private fun ShortClassName.toApiShortClassName() = mapOutermostClassName { "I$it" }

    fun QualifiedName.toApiClass(): QualifiedName = if (isMcClassName()) {
        copy(
            packageName = packageName.toApiPackageName(),
            shortName = shortName.toApiShortClassName()
        )
    } else this

    private fun ShortClassName.toBaseShortClassName() = mapOutermostClassName { "Base$it" }

    //TODO: refactor with new NameMappable
    fun QualifiedName.toBaseClass(): QualifiedName =
        copy(
            packageName = packageName.toApiPackageName(),
            shortName = shortName.toBaseShortClassName()
        )

    fun <T : NameMappable<T>> T.remapToApiClass(): T = map { it.toApiClass() }
    fun <T : NameMappable<T>> List<T>.remapToApiClassesOld(): List<T> = mapElementsOld { it.toApiClass() }
    fun <T : Mappable<T>> T.remapToApiClass(): T = map { it.toApiClass() }
    fun <T : Mappable<T>> List<T>.remapToApiClasses(): List<T> = mapElements { it.toApiClass() }
//    fun <T : GenericReturnType> JavaType<T>.remapToApiClass(): JavaType<T> = map { it.toApiClass() }
//
//    fun <T : GenericReturnType> T.remapToApiClass(): T = remap { it.toApiClass() }
//    fun List<TypeArgumentDeclaration>.remapDeclToApiClasses() = map { typeArg ->
//        typeArg.copy(
//            classBound = typeArg.classBound?.remapToApiClass(),
//            interfaceBounds = typeArg.interfaceBounds.map { it.remapToApiClass() })
//    }
//
//
//    fun <T : GenericReturnType> List<JavaType<T>>.remapToApiClasses(): List<JavaType<T>> =
//        map { it.remapToApiClass() }
}

fun PackageName?.isMcPackage(): Boolean = if (this == null) false
else startsWith("net", "minecraft") || startsWith("com", "mojang", "blaze3d")

fun QualifiedName.isMcClassName(): Boolean = packageName.isMcPackage()
fun GenericReturnType.isMcClass(): Boolean = this is ArrayGenericType && componentType.isMcClass() ||
        this is ClassGenericType && packageName.isMcPackage()
        || this is TypeVariable && toJvmType().isMcClass()

private fun JvmType.isMcClass(): Boolean = this is ObjectType && fullClassName.isMcClassName()
        || this is ArrayType && componentType.isMcClass()

fun JavaType<*>.isMcClass(): Boolean = type.isMcClass()