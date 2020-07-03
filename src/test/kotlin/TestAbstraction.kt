import abstractor.VersionPackage
import asm.readToClassNode
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.junit.jupiter.api.Test
import org.objectweb.asm.util.ASMifier
import org.objectweb.asm.util.TraceClassVisitor
import metautils.testing.getResource
import metautils.util.*
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider


class TestAbstraction {

    @Test
    fun testAbstraction() {

        val mcJar = getResource("testOriginalJar.jar")
//        val dest = mcJar.parent.resolve("abstractedSrc")
        val implDest = mcJar.parent.resolve("abstractedAsm")
        val apiDest = mcJar.parent.resolve("abstractedAsmApi")


        val metadata = AbstractionMetadata(
            versionPackage = VersionPackage("v1"),
            classPath = listOf(), fitToPublicApi = false, writeRawAsm = true
        )
        Abstractor.abstract(mcJar, implDest, metadata = metadata)
        val manifest = Abstractor.abstract(mcJar, apiDest, metadata = metadata.copy(fitToPublicApi = true))
        val manifestJson = Json(
            JsonConfiguration(prettyPrint = true)
        ).stringify(
            MapSerializer(String.serializer(), AbstractedClassInfo.serializer()),
            manifest
        )
        Paths.get("testdata").createDirectories()
        Paths.get("testdata/abstractionManifest.json").writeString(manifestJson)


        implDest.convertDirToJar()
        implDest.recursiveChildren().forEach { if (it.isClassfile()) printAsmCode(it) }

        val apiJar = apiDest.convertDirToJar()
        val apiSrcDest = mcJar.parent.resolve("abstractAsmApi-sources.jar")
        ForgedFlower.decompile(
            preferences = ForgedFlower.Preferences(),
            input = apiJar,
            output = apiSrcDest,
            javaDocs = getResource("mappings.tiny"),
            libraries = listOf(getResource("testOriginalJar.jar")),
            lineMap = Paths.get("linemap")
        )
    }

    @Test
    fun testForgedFlower() {
        val sources = Paths.get("abstractedAsmApi-sources.jar")
        ForgedFlower.decompile(
            preferences = ForgedFlower.Preferences(),
            input = Paths.get("build\\resources\\test\\abstractedAsmApi.jar"),
            output = sources,
            javaDocs = getResource("mappings.tiny"),
            libraries = listOf(Paths.get("testdata/mcJarWithInterfaces.jar")),
            lineMap = Paths.get("linemap")
        )

        sources.unzipJar()
    }

    private fun verifyJava(dest: Path) {
        val compiler = ToolProvider.getSystemJavaCompiler()

        val diagnostics = DiagnosticCollector<JavaFileObject>()
        compiler.getStandardFileManager(diagnostics, null, null).use { fileManager ->
            val compilationUnits = fileManager.getJavaFileObjectsFromFiles(
                dest.recursiveChildren().filter { !it.isDirectory() }.map { it.toFile() }.toList()
            )

            val runtime = getResource("apiRuntime.jar").toAbsolutePath().toString()
            val mcJarWithInterfaces = Paths.get("testdata/mcJarWithInterfaces.jar").toAbsolutePath().toString()
            val classpath = "$mcJarWithInterfaces;$runtime"
            println("Compiling with classpath = $classpath")
            compiler.getTask(
                null, fileManager, diagnostics,
                listOf("-classpath", /*System.getProperty("java.class.path")*/classpath), null, compilationUnits
            ).call()
        }

        val compiledDestDir = Paths.get("${dest}Compiled")
        compiledDestDir.createDirectories()
        dest.recursiveChildren()
            .filter { it.isClassfile() }
            //            .take(1)
            .forEach {
                printAsmCode(it)
                val relativePath = dest.relativize(it)
                val target = compiledDestDir.resolve(relativePath.toString())
                target.parent.createDirectories()
                if (!it.isDirectory()) {
                    it.copyTo(target)
                    it.delete()
                }
            }
        compiledDestDir.convertDirToJar()

        assert(diagnostics.diagnostics.none { it.kind == Diagnostic.Kind.ERROR }) {
            "Compilation errors exist: \n" + diagnostics.diagnostics.joinToString("\n\n") + "\n"
        }
    }


}

private fun printAsmCode(path: Path) {
    val target = Paths.get(path.toString().removeSuffix(".class") + "_asm.java")
//    println("Writing asm to $target")
    Files.newOutputStream(target).use {
        val visitor = TraceClassVisitor(null, ASMifier(), PrintWriter(it))
        readToClassNode(path).accept(visitor)
    }

}