import api.ClassApi
import api.readFromDirectory
import java.nio.file.Path

object Abstractor {
    fun abstract(mcJar: Path, destJar: Path) {
        require(destJar.parent.exists()) { "The chosen destination path '$destJar' is not in any existing directory." }
        require(destJar.parent.isDirectory()) { "The parent of the chosen destination path '$destJar' is not a directory." }

        destJar.deleteIfExists()
        destJar.createJar()

        destJar.openJar { destFs ->
            mcJar.walkJar { mcPath ->
                if (mcPath.isDirectory()) {
                    for (apiClass in ClassApi.readFromDirectory(mcPath)) {

                    }
                }
            }
        }
    }
}

private fun abstractClass(api: ClassApi, destPath: Path) {

}