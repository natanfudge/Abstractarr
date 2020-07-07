import kotlinx.coroutines.runBlocking
import metautils.asm.readToClassNode
import metautils.testing.getResource
import metautils.util.*
import kotlin.system.measureTimeMillis

fun main() {
    val time = measureTimeMillis {
        TestAbstraction().testMc()
    }
    println("Millis = $time")
}