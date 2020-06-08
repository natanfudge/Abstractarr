import asm.readToClassNode
import descriptor.JavaLangObject
import descriptor.MethodDescriptor
import org.objectweb.asm.tree.ClassNode
import java.nio.file.Path

data class MethodEntry(val name: String, val descriptor: String)

data class ClassEntry(val methods: Set<MethodEntry>, val superClass: String?, val superInterfaces: List<String>)

private val ClassEntry.directSuperTypes: List<String> get() = if(superClass != null) superInterfaces + superClass else superInterfaces

/**
 * class names use slash/separated/format
 */
data class ClasspathIndex(private val classes: Map<String, ClassEntry>) {
    private fun verifyClassName(name: String) {
        require(classes.containsKey(name)) {
            "Attempt to find class not in the specified classpath: $name"
        }
    }

    fun classHasMethod(className: String, methodName: String, methodDescriptor: MethodDescriptor): Boolean {
        verifyClassName(className)
        return classes.getValue(className).methods.contains(MethodEntry(methodName, methodDescriptor.classFileName))
    }

    fun getSuperTypesRecursively(className: String) : Set<String> {
        return (getSuperTypesRecursivelyImpl(className) + JavaLangObject).toSet()
    }

    private fun getSuperTypesRecursivelyImpl(className: String): List<String> {
        verifyClassName(className)
        val directSupers = classes.getValue(className).directSuperTypes
        return (directSupers + directSupers.filter { it != JavaLangObject }.flatMap { getSuperTypesRecursivelyImpl(it) })
    }
}


@OptIn(ExperimentalStdlibApi::class)
fun indexClasspath(classPath: List<Path>): ClasspathIndex {
    val map = classPath.flatMap { path ->
        getClasses(path).map { classNode ->
            classNode.name to ClassEntry(
                methods = classNode.methods.map { MethodEntry(it.name, it.desc) }.toHashSet(),
                superClass = classNode.superName, superInterfaces = classNode.interfaces
            )
        }
    }.toMap()
    return ClasspathIndex(map)
}

private fun getClasses(path: Path): List<ClassNode> = when {
    !path.exists() -> listOf()
    path.isDirectory() -> path.recursiveChildren().filter { it.isClassfile() }.map { readToClassNode(it) }.toList()
    path.toString().endsWith(".jar") -> path.walkJar { paths ->
        paths.filter { it.isClassfile() }.map { readToClassNode(it) }.toList()
    }
    else -> error("Got a classpath element which is not a jar or directory: $path")
}
