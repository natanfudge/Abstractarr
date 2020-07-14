import abstractor.*
import metautils.asm.readToClassNode
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import metautils.api.isInnerClass
import metautils.api.isProtected
import metautils.descriptor.JvmPrimitiveType
import metautils.descriptor.MethodDescriptor
import metautils.descriptor.ObjectType
import metautils.descriptor.ReturnDescriptor
import org.junit.jupiter.api.Test
import org.objectweb.asm.util.ASMifier
import org.objectweb.asm.util.TraceClassVisitor
import metautils.testing.getResource
import metautils.testing.verifyClassFiles
import metautils.util.*
import org.junit.jupiter.api.Disabled
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
        val implDest = mcJar.parent.resolve("abstractedAsm")
        val apiDest = mcJar.parent.resolve("abstractedAsmApi")
        val apiSrcDest = mcJar.parent.resolve("abstractAsmApi-sources")


        val metadata = AbstractionMetadata(
            versionPackage = VersionPackage("v1"),
            classPath = listOf(), fitToPublicApi = false, writeRawAsm = true,
            selector = TargetSelector.All,
            javadocs = testJavadocs()
        )
        val manifest = Abstractor.parse(mcJar, metadata) { abstractor ->
            abstractor.abstract(implDest, metadata)
            abstractor.abstract(apiDest, metadata = metadata.copy(fitToPublicApi = true))
            abstractor.abstract(apiSrcDest, metadata = metadata.copy(fitToPublicApi = true, writeRawAsm = false))
        }

        val manifestJson = Json(
            JsonConfiguration(prettyPrint = true)
        ).stringify(
            MapSerializer(String.serializer(), AbstractedClassInfo.serializer()),
            manifest
        )

        verifyClassFiles(implDest, listOf(mcJar))

        Paths.get("testdata").createDirectories()
        Paths.get("testdata/abstractionManifest.json").writeString(manifestJson)


        implDest.convertDirToJar()
        implDest.recursiveChildren().forEach { if (it.isClassfile()) printAsmCode(it) }

        apiDest.convertDirToJar()
        apiSrcDest.convertDirToJar()

    }


    @Test
//    @Disabled
    fun testMc() {
        val mcJar = getResource("minecraft-1.16.1.jar")
        val implDest = mcJar.parent.resolve("abstractedMcImpl")
        val apiDest = mcJar.parent.resolve("abstractedMcApi")
        val srcDest = mcJar.parent.resolve("abstractMcApi-sources")
        val classpath = getResource("mclibs").recursiveChildren().filter { it.hasExtension(".jar") }.toList()
        val metadata = AbstractionMetadata(
            versionPackage = VersionPackage("v1"),
            classPath = classpath, fitToPublicApi = false, writeRawAsm = true,
            selector = TargetSelector.All,
            javadocs = JavaDocs.readTiny(getResource("yarn-1.16.1+build.5-v2.tiny"))
        )
        val manifest = Abstractor.parse(mcJar, metadata) {
            it.abstract(implDest, metadata)
            it.abstract(apiDest, metadata = metadata.copy(fitToPublicApi = true))
            it.abstract(srcDest, metadata = metadata.copy(fitToPublicApi = true, writeRawAsm = false))
        }

        val manifestJson = Json(
            JsonConfiguration(prettyPrint = true)
        ).stringify(AbstractionManifestSerializer, manifest)

        Paths.get("mcmanifest.json").writeString(manifestJson)

        verifyClassFiles(implDest, classpath + listOf(mcJar))
        implDest.recursiveChildren().forEach { if (it.isClassfile()) printAsmCode(it) }

    }

    private fun testJavadocs(): JavaDocs {
        val testClass = Documentable.Class("net/minecraft/TestConcreteClass".toQualifiedName(dotQualified = false))
        val testField = Documentable.Field(testClass, "publicField")
        val testConstructor = Documentable.Method(
            testClass, "<init>", MethodDescriptor(
                listOf(
                    JvmPrimitiveType.Int,
                    ObjectType("net/minecraft/TestOtherClass".toQualifiedName(dotQualified = false))
                ),
                ReturnDescriptor.Void
            )
        )
        val testConstructorParam = Documentable.Parameter(testConstructor, 1)
        val testMethod = Documentable.Method(
            testClass, "publicInt", MethodDescriptor(
                listOf(
                    ObjectType("net/minecraft/TestOtherClass".toQualifiedName(dotQualified = false))
                ),
                JvmPrimitiveType.Int
            )
        )
        val testMethodParam = Documentable.Parameter(testMethod, 0)

        val testProtectedMethod = Documentable.Method(
            testClass, "protectedStaticParam",
            MethodDescriptor(listOf(JvmPrimitiveType.Int), ReturnDescriptor.Void)
        )

        val testProtectedMethodParam = Documentable.Parameter(testProtectedMethod, 0)

        val testInnerClass = Documentable.Class(
            "net/minecraft/TestConcreteClass\$TestInnerClass".toQualifiedName(dotQualified = false)
        )

        val testInnerClassConstructor = Documentable.Method(
            testInnerClass, "<init>", MethodDescriptor(
                listOf(
                    JvmPrimitiveType.Int,
                    ObjectType("net/minecraft/TestOtherClass".toQualifiedName(dotQualified = false))
                ),
                ReturnDescriptor.Void
            )
        )

        val testInnerClassConstructorParam1 = Documentable.Parameter(testInnerClassConstructor, 1)
        val testInnerClassConstructorParam2 = Documentable.Parameter(testInnerClassConstructor, 0)

        return JavaDocs(
            classes = mapOf(
                testClass to "This is a class foo bar",
                testInnerClass to "This is an inner class baz biz"
            ),
            fields = mapOf(testField to "Field of hell that will have getters and setters"),
            methods = mapOf(
                testConstructor to "Outer class constructor wow",
                testMethod to "Outer class method wow",
                testInnerClassConstructor to "This inner class constructor is constructed by the outer class!",
                testProtectedMethod to "Protected method only in baseclass!"
            ),
            parameters = mapOf(
                testConstructorParam to "Awesome constructor param of hell",
                testMethodParam to "Even better method param",
                testInnerClassConstructorParam1 to "Mojang using it is propaganda",
                testInnerClassConstructorParam2 to "No one uses inner classes lul",
                testProtectedMethodParam to "Secret param of protected"
            )
        )
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