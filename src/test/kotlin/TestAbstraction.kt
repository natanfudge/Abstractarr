import org.junit.jupiter.api.Test
import testing.getResource
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider


class TestAbstraction {

    @Test
    fun testAbstraction() {

        val mcJar = getResource("testOriginalJar.jar")
        val dest = mcJar.parent.resolve("abstractedSrc")
        Abstractor.abstract(mcJar, dest, metadata = AbstractionMetadata(versionPackage = "v1"))
//        debugResultJar(dest)

        val compiler = ToolProvider.getSystemJavaCompiler()

        val diagnostics = DiagnosticCollector<JavaFileObject>()
        compiler.getStandardFileManager(diagnostics, null, null).use { fileManager ->
            val compilationUnits = fileManager.getJavaFileObjectsFromFiles(
                dest.recursiveChildren().filter { !it.isDirectory() }.map { it.toFile() }.toList()
            )

            try {
                compiler.getTask(
                    null, fileManager, diagnostics,
                    listOf("-classpath", System.getProperty("java.class.path")), null, compilationUnits
                ).call()

                assert(diagnostics.diagnostics.isEmpty()) {
                    "Compilation errors exist: \n" + diagnostics.diagnostics.joinToString("\n\n") + "\n"
                }
            } finally {
                dest.recursiveChildren().forEach { if (it.hasExtension(".class")) it.delete() }
            }

        }


//        for (diagnostic in diagnostics.diagnostics) System.out.format(
//            "Error on line %d in %s%n",
//            diagnostic.lineNumber,
//            diagnostic.source.toUri()
//        )

    }

}

