import org.junit.jupiter.api.Test
import testing.getResource
import util.*
import java.nio.file.Paths
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider



class TestAbstraction {


    @Test
    fun testAbstraction() {

        val mcJar = getResource("testOriginalJar.jar")
        val dest = mcJar.parent.resolve("abstractedSrc")
//        val classPath = System.getProperty("java.class.path").split(';').map { Paths.get(it) }
        Abstractor.abstract(mcJar, dest, metadata = AbstractionMetadata(versionPackage = "v1", classPath = listOf()))

        val compiler = ToolProvider.getSystemJavaCompiler()

        val diagnostics = DiagnosticCollector<JavaFileObject>()
        compiler.getStandardFileManager(diagnostics, null, null).use { fileManager ->
            val compilationUnits = fileManager.getJavaFileObjectsFromFiles(
                dest.recursiveChildren().filter { !it.isDirectory() }.map { it.toFile() }.toList()
            )

            val runtime = getResource("apiRuntime.jar").toAbsolutePath().toString()
            val mcJar = getResource("mcJarWithInterfaces.jar").toAbsolutePath().toString()
            compiler.getTask(
                null, fileManager, diagnostics,
                listOf("-classpath", /*System.getProperty("java.class.path")*/ "$mcJar;$runtime"), null, compilationUnits
            ).call()
        }


        val compiledDestDir = Paths.get("${dest}Compiled")
        compiledDestDir.createDirectories()
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
        compiledDestDir.convertDirToJar()

        assert(diagnostics.diagnostics.none { it.kind == Diagnostic.Kind.ERROR }) {
            "Compilation errors exist: \n" + diagnostics.diagnostics.joinToString("\n\n") + "\n"
        }
    }


}

