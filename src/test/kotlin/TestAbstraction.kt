import org.junit.jupiter.api.Test
import testing.getResource
import java.nio.file.Paths
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider


class TestAbstraction {

    @Test
    fun testAbstraction() {

        val mcJar = getResource("testOriginalJar.jar")
        val dest = mcJar.parent.resolve("abstractedSrc")
        Abstractor.abstract(mcJar, dest, metadata = AbstractionMetadata(versionPackage = "v1"))

        val compiler = ToolProvider.getSystemJavaCompiler()

        val diagnostics = DiagnosticCollector<JavaFileObject>()
        compiler.getStandardFileManager(diagnostics, null, null).use { fileManager ->
            val compilationUnits = fileManager.getJavaFileObjectsFromFiles(
                dest.recursiveChildren().filter { !it.isDirectory() }.map { it.toFile() }.toList()
            )

            compiler.getTask(
                null, fileManager, diagnostics,
                listOf("-classpath", System.getProperty("java.class.path")), null, compilationUnits
            ).call()
        }


        val compiledDestDir = Paths.get("${dest}Compiled")
        dest.recursiveChildren().forEach {
            if (it.hasExtension(".class")) {
                val relativePath = dest.relativize(it)
                val target = compiledDestDir.resolve(relativePath.toString())
                target.parent.createDirectories()
                if (!it.isDirectory()) {
                    it.copyTo(target)
                    it.delete()
                }
            }
        }
        compiledDestDir.zipToJar()

        assert(diagnostics.diagnostics.isEmpty()) {
            "Compilation errors exist: \n" + diagnostics.diagnostics.joinToString("\n\n") + "\n"
        }
    }


}

