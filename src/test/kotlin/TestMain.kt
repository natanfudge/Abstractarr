import metautils.util.directChildren
import java.nio.file.Paths

fun main() {
    val str = Paths.get("C:\\Users\\natan\\Desktop\\Abstractarr\\src\\testOriginalJar\\java\\net\\minecraft")
        .directChildren().joinToString {"\"${it.fileName.toString().removeSuffix(".java")}\""}
    println(str)
}