package abstractor

import metautils.asm.opCode
import metautils.api.*
import metautils.signature.ArrayGenericType
import metautils.signature.ClassGenericType
import metautils.util.ClasspathIndex
import org.objectweb.asm.Opcodes


@Suppress("UNUSED_PARAMETER")
internal fun doubleCastRequired(classApi: ClassApi) =
    false /*true*/ /*classApi.isFinal*/ // the rules seem too ambiguous


internal fun ClassApi.Method.isOverride(index: ClasspathIndex, owningClass: ClassApi) = !isConstructor && !isStatic &&
        index.getSuperTypesRecursively(owningClass.name)
            .any { index.getMethod(it, name, getJvmDescriptor()) != null }

internal fun ClassApi.Method.isOnlyImplementingOverride(index: ClasspathIndex, owningClass: ClassApi): Boolean {
    if (isConstructor || isStatic || isAbstract) return false
    val overriding = index.getSuperTypesRecursively(owningClass.name)
        .mapNotNull { index.getMethod(it, name, getJvmDescriptor()) }
    if (overriding.isEmpty()) return false
    return overriding.all { it.access opCode Opcodes.ACC_ABSTRACT }
}
//    !isConstructor && !isStatic && index.getSuperTypesRecursively(owningClass.name)
//            .any {classEntry -> index.getMethod(classEntry, name, getJvmDescriptor()).let { it != null && }   }

//internal fun ClassApi.Method.isOverrideIgnoreReturnType(index: ClasspathIndex, owningClass : ClassApi) = !isConstructor && !isStatic &&
//        index.getSuperTypesRecursively(owningClass.name)
//            .any { index.classHasMethodIgnoringReturnType(it, name, getJvmDescriptor()) }

// Since inner classes are converted to interfaces, they become static, so they must contain the type arguments of their outer classes
// with them.
internal fun JavaClassType.pushAllTypeArgumentsToInnermostClass(): JavaClassType =
    copy(type = type.pushAllTypeArgumentsToInnermostClass())

internal fun ClassGenericType.pushAllTypeArgumentsToInnermostClass(): ClassGenericType {
    val allArgs = classNameSegments.flatMap { it.typeArguments  }
    val modifiedSegments = classNameSegments.mapIndexed { index, segment ->
        segment.copy(typeArguments =
        if (index == classNameSegments.size - 1) allArgs.let { if (it.isEmpty()) listOf() else it } else listOf()
        )
    }
    return copy(classNameSegments = modifiedSegments)
}

//soft to do: this is a bad solution. What we need to do is have remapToApiClass go through the tree and selectively
// push type arguments to the end when it detects an inner api class.
// for now it will do though because inner classes are rare in MC
internal fun JavaType<ArrayGenericType>.pushAllArrayTypeArgumentsToInnermostClass() =
    copy(
        type = type.copy(componentType = type.componentType
            .let { if (it is ClassGenericType) it.pushAllTypeArgumentsToInnermostClass() else it })
    )