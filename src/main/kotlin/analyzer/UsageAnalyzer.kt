package analyzer

import metautils.api.JavaAnnotation
import metautils.asm.readToClassNode
import metautils.types.MethodDescriptor
import metautils.signature.*
import metautils.types.JvmType
import metautils.util.*
import org.objectweb.asm.tree.*
import java.nio.file.Path
import java.nio.file.Paths


fun main() {
    analyze(Paths.get("C:\\Users\\natan\\Desktop\\Abstractarr\\spatialcrafting-1.4.2+20w19a-dev.jar"),
            Paths.get("C:\\Users\\natan\\Desktop\\Abstractarr\\analysis-keys.csv"),
        Paths.get("analysis-values.csv"))
}

private fun analyze(jar: Path, destKeys: Path, destValues: Path) {
    check(jar.exists())
    val classfiles = jar.walkJar { seq ->
        seq.filter { it.isClassfile() }.map {
//            println(it)
            readToClassNode(it)
        }.toList()
    }
    val statistics = classfiles.flatMap { it.usages() }
        .filterIsInstance<Usage.Class>()
        .filter { it.name.packageStartsWith("net", "minecraft") }
        .groupBy { it.name }
        .entries
        .sortedByDescending { it.value.size }
        .map {
            Statistic(it.key.toString(), it.value.size) }
//        .joinToString(", ") { (name, usages) -> name.presentableName + ":" + usages.size.toString() }

    destKeys.writeString(statistics.humanReadable())
//    destValues.writeString(statistics.spaceSeparatedValues())
}

private data class Statistic(val label: String, val value: Int)

private fun List<Statistic>.spaceSeparatedLabels() = joinToString(" ") { it.label }
private fun List<Statistic>.spaceSeparatedValues() = joinToString(" ") { it.value.toString() }
private fun List<Statistic>.humanReadable() = joinToString("\n") { it.label + ": " + it.value }

private sealed class Usage {
    data class Class(val name: QualifiedName) : Usage()
    data class Method(val className: QualifiedName, val name: String, val descriptor: MethodDescriptor) : Usage()
    data class Field(val className: QualifiedName, val name: String) : Usage()
}

private fun ClassNode.usages(): List<Usage> {
    val topLevel = (interfaces + superName).map { it.toSlashQualifiedName() }.map { Usage.Class(it) } +
            classSignatureUsages(signature)
    val fieldUsages = fields.flatMap { it.usages() }
//    val annotationUsages = (visibleAnnotations.orEmpty() + visibleTypeAnnotations.orEmpty() +
//            invisibleAnnotations.orEmpty() + invisibleTypeAnnotations.orEmpty())
//        .flatMap { it.usages() }
    val annotationUsages = annotationUsages(
        visibleAnnotations, visibleTypeAnnotations, invisibleAnnotations, invisibleTypeAnnotations
    )

    val methodUsages = methods.flatMap { it.usages() }

    return topLevel + fieldUsages + annotationUsages + methodUsages
}

private fun classSignatureUsages(signature: String?) =
        signatureUsages(signature) { ClassSignature.readFrom(it, null) }

private fun FieldNode.usages(): List<Usage> {
    val annotationUsages = annotationUsages(
        visibleAnnotations, visibleTypeAnnotations, invisibleAnnotations, invisibleTypeAnnotations
    )

    val signatureUsages = if (signature != null) {
        fieldSignatureUsages(signature)
    } else fieldDescriptorUsages(desc)

    return annotationUsages + signatureUsages
}

private fun fieldSignatureUsages(signature: String?) =
        signatureUsages(signature) { FieldSignature.readFrom(it, null) }

private fun fieldDescriptorUsages(descriptor: String) =
    JvmType.fromDescriptorString(descriptor).getContainedNamesRecursively().map { Usage.Class(it) }

private fun MethodNode.usages(): List<Usage> {
    val annotationUsages = annotationUsages(
        visibleAnnotations, visibleTypeAnnotations, invisibleAnnotations, invisibleTypeAnnotations
    )
    val parameterAnnotationUsages = annotationUsages(
        visibleParameterAnnotations?.flatMap { it.orEmpty() }, invisibleParameterAnnotations?.flatMap { it.orEmpty() }
    )

    val signatureUsages = if (signature != null) {
        methodSignatureUsages(signature)
    } else methodDescriptorUsages(desc)

    val exceptionUsages = exceptions.map { Usage.Class(it.toSlashQualifiedName()) }

    val instructionUsages = instructions.flatMap { it.usages() }

    return annotationUsages + parameterAnnotationUsages + signatureUsages + exceptionUsages + instructionUsages
}

private fun methodDescriptorUsages(descriptor: String) =
        MethodDescriptor.fromDescriptorString(descriptor).getContainedNamesRecursively().map { Usage.Class(it) }

private fun methodSignatureUsages(signature: String?) =
        signatureUsages(signature) { MethodSignature.readFrom(it, null) }

private fun AbstractInsnNode.usages(): List<Usage> {
    val annotationUsages = annotationUsages(visibleTypeAnnotations, invisibleTypeAnnotations)
    val instructionUsages = when (this) {
        is FieldInsnNode -> {
           fieldDescriptorUsages(desc) +  Usage.Field(className = owner.toSlashQualifiedName(), name = name)
        }
//        is InvokeDynamicInsnNode ->{
//            Usage.Method(className = this.)
//        }
        is MethodInsnNode -> {
            methodDescriptorUsages(desc) + Usage.Method(
                className = owner.toSlashQualifiedName(),
                name = name,
                descriptor = MethodDescriptor.fromDescriptorString(desc)
            )
        }
        is TypeInsnNode -> {
             listOf(Usage.Class(name = desc.toSlashQualifiedName()))
        }
        else -> listOf()
    }
    return annotationUsages + instructionUsages
}

private fun annotationUsages(vararg annotationLists: List<AnnotationNode>?): List<Usage> =
    annotationLists.flatMap { list -> list.orEmpty().flatMap { it.usages() } }

private fun signatureUsages(signature: String?, signatureParser: (String) -> Signature) =
    if (signature == null) listOf() else
        signatureParser(signature).getContainedNamesRecursively()
            .map { Usage.Class(it) }

private fun AnnotationNode.usages(): List<Usage> {
    return JavaAnnotation.fromAsmNode(this).getContainedNamesRecursively().map { Usage.Class(it) }
}