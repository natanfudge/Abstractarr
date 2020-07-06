import metautils.asm.readToClassNode
import metautils.testing.getResource
import metautils.util.*

fun main() {
//    val root = Paths.get("src/test/resources/mclibs")
//    val libs = Minecraft.downloadLibraryUrlsFor(version = "1.16.1")
//    for ((url, path) in libs) {
//        val dest = root.resolve(path)
//        println("Downloading $url to $path")
//        dest.createParentDirectories()
//        dest.deleteIfExists()
//        downloadJarFromUrl(url,dest)
//    }


    TestAbstraction().testMc()

//    val jar = getResource("minecraft-1.16.1.jar")
//    val path = jar.openJar { it.getPath("net/minecraft/block/Block.class") }
//    val classNode = readToClassNode(path)
//    val y = 2
}