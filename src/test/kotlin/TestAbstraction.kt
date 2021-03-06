import abstractor.VersionPackage
import asm.readToClassNode
import org.junit.jupiter.api.Test
import org.objectweb.asm.util.ASMifier
import org.objectweb.asm.util.TraceClassVisitor
import testing.getResource
import util.*
import java.io.PrintWriter
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider


class TestAbstraction {


    private val classes = listOf(
        "ExtendedInterface", "ExtendedInterfaceRaw", "TestAbstractClass", "TestAbstractImpl",
        "TestAnnotations", "TestArrays", "TestClashingNames", "TestConcreteClass", "TestEnum", "TestFinalClass",
        "TestGenerics", "TestInnerExtender", "TestInterface", "TestLambdaInterface", "TestLambdasAnons",
        "TestNormalClassExtender", "TestOtherClass", "TestOverload", /*"TestOverrideReturnTypeChange",
        "TestOverrideReturnTypeChangeSuper",*/ "TestSuperClass", "TestThrows"
    )

    private val noBase = listOf(
       "TestEnum", "TestFinalClass"
    )

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
//        Abstractor.abstract(mcJar, dest, metadata = metadata)
        Abstractor.abstract(mcJar, implDest, metadata = metadata)
        Abstractor.abstract(mcJar, apiDest, metadata = metadata.copy(fitToPublicApi = true))


        val asmJar = implDest.convertDirToJar()
        implDest.recursiveChildren().forEach { if (it.isClassfile()) printAsmCode(it) }
        verifyBytecode(asmJar)

        apiDest.convertDirToJar()

//        verifyJava(dest)
    }

    private fun verifyBytecode(asmJar: Path) {
        val mcJarWithInterfaces = Paths.get("testdata/mcJarWithInterfaces.jar")
        val classLoader = URLClassLoader(
            arrayOf(asmJar.toUri().toURL(), mcJarWithInterfaces.toUri().toURL())
            /* , this::class.java.classLoader*/
        )
        classes.forEach {
            try {
                Class.forName("v1.net.minecraft.I$it", true, classLoader)
            } catch (e: Throwable) {
                println("Error in interface of $it:")
                throw e
            }
            try {
                if (it !in noBase) Class.forName("v1.net.minecraft.Base$it", true, classLoader)
            } catch (e: Throwable) {
                println("Error in baseclass of $it:")
                throw e
            }

        }
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