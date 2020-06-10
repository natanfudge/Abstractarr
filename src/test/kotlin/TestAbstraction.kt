import org.junit.jupiter.api.Test
import testing.getResource
import java.nio.file.Paths
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider



class TestAbstraction {

//    private fun buildClasspathFromString(classpath : String) : List<Path>{
//       return classpath.split(';').map {
//           if(it)
//       }
//
//    }

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
            compiler.getTask(
                null, fileManager, diagnostics,
                listOf("-classpath", System.getProperty("java.class.path") + ";" + runtime), null, compilationUnits
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

        assert(diagnostics.diagnostics.filter { it.kind == Diagnostic.Kind.ERROR }.isEmpty()) {
            "Compilation errors exist: \n" + diagnostics.diagnostics.joinToString("\n\n") + "\n"
        }
    }


}

