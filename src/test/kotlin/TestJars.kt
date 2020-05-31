import org.junit.jupiter.api.Test
import testing.debugResultJar

class TestJars {

    @Test
    fun testAbstraction() {
        val mcJar = getResource("testOriginalJar.jar")
        val dest = mcJar.parent.resolve("abstractedJar.jar")
        Abstractor.abstract(mcJar, dest, metadata = AbstractionMetadata(versionPackage = "v1"))
        debugResultJar(dest)

//        testJar(dest) {
//
//        }
    }

}

