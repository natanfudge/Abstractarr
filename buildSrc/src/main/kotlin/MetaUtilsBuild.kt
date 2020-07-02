import abstractor.VersionPackage
import api.ClassApi
import asm.readToClassNode
import asm.writeTo
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import metautils.api.isThrowable
import metautils.api.outerClassesToThis
import metautils.api.readFromList
import metautils.api.visitThisAndInnerClasses
import metautils.signature.*
import metautils.util.toPath
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import util.convertDirToJar
import util.createDirectories
import util.isDirectory
import util.recursiveChildren
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path


class MetaUtils : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("metaUtils", BuildMetaUtilsExtension::class.java, project)
    }
}

/** A list used in testing and production to know what interfaces/classes to attach to minecraft interface */
//typealias AbstractionManifest = Map<String, AbstractedClassInfo>

@Serializable
data class AbstractedClassInfo(val apiClassName: String, /*val isThrowable: Boolean, */val newSignature: String)

open class BuildMetaUtilsExtension(private val project: Project) {
    fun createJarTest(name: String): SourceSet = with(project) {

        val sourceSet = sourceSets.create(name)
        val jarTask = tasks.create(name, Jar::class.java) { task ->
            group = "testing"
            task.from(sourceSet.output)

            task.destinationDirectory.set(sourceSets.getByName("test").output.resourcesDir)
            task.archiveFileName.set("$name.jar")
        }

        tasks.named("processTestResources") { task ->
            task.dependsOn(jarTask)
        }

        return@with sourceSet
    }


    fun createAttachInterfacesTask(targetClassDirs: Set<File>): FileCollection = with(project) {
        val targetClassDir = targetClassDirs.first { it.parentFile.name == "java" }
        val targetClassPath = targetClassDir.toPath()
        val destinationJar = project.file("testdata/mcJarWithInterfaces.jar")
        tasks.create("attachInterfaces") { task ->
            task.doLast {
                val inputChildren = targetClassPath.recursiveChildren()
                val allInputs = inputChildren.filter { !it.isDirectory() }.toList()
                val outputDir = project.file("testdata/mcJarWithInterfaces").toPath()
                val abstractionManifestFile = project.file("testdata/abstractionManifest.json").toPath()
                val abstractionManifest = Json(JsonConfiguration.Stable).parse(
                    MapSerializer(String.serializer(), AbstractedClassInfo.serializer()),
                    Files.readAllBytes(abstractionManifestFile).toString(Charset.defaultCharset())
                )

                val outputsToInputs = allInputs.associateBy { path ->
                    val relativePath = targetClassPath.relativize(path).toString()
                    outputDir.resolve(relativePath)
                }
                for ((output, input) in outputsToInputs) {
                    val classNode = readToClassNode(input)
                    check(classNode.name.startsWith("net/minecraft/"))

                    val abstractionEntry = abstractionManifest[classNode.name]
                    if (abstractionEntry != null) {
                        val newName = abstractionEntry.apiClassName
//                        if (abstractionEntry.isThrowable) {
//                            println("Replacing superclass of ${classNode.name} from ${classNode.superName} to $newName")
//                            classNode.superName = newName
//                        } else {
                            println("Attaching interface $newName to ${classNode.name}")
                            classNode.interfaces.add(newName)
//                        }
                        if (classNode.signature != null) {
                            classNode.signature = abstractionEntry.newSignature
                        }
                    }


                    output.parent.createDirectories()

                    classNode.writeTo(output)
                }




                destinationJar.toPath().parent.createDirectories()
                outputDir.convertDirToJar(destination = destinationJar.toPath())
            }
        }
        files(destinationJar)
    }

}

//private fun Collection<ClassApi>.matchToPaths(rootPath: Path): Map<Path, ClassApi> =
//    mutableMapOf<Path, ClassApi>().apply {
//        for (topLevelClass in this@matchToPaths) {
//            topLevelClass.visitThisAndInnerClasses { classApi ->
//                val path = rootPath.resolve(classApi.name.packageName.toPath())
//                    .resolve(classApi.name.shortName.toDollarQualifiedString() + ".class")
//                put(path, classApi)
//            }
//        }
//    }
//
//
//private fun VersionPackage.allApiClassTypeArguments(classApi: ClassApi): List<TypeArgumentDeclaration> = when {
//    classApi.isStatic -> classApi.typeArguments.remapDeclToApiClasses()
//    else -> classApi.outerClassesToThis().flatMap { it.typeArguments.remapDeclToApiClasses() }
//}

private val Project.sourceSets: SourceSetContainer
    get() = convention.getPlugin(JavaPluginConvention::class.java).sourceSets