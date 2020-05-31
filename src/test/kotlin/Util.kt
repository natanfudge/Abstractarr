import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.reflect.Member
import java.lang.reflect.Modifier
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile

fun getResource(path: String) = Paths.get(
    TestJars::class.java
        .classLoader.getResource("dummyResource")!!.toURI()
).parent.resolve(path)

