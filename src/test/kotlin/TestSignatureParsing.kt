import metautils.asm.readToClassNode
import metautils.signature.*
import metautils.testing.getResource
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@Suppress("UNUSED_VARIABLE")
class TestSignatureParsing {
    @Test
    fun testSignatures() {
        val classNode = getResource("GenericsTest.class") { readToClassNode(it) }
        val classSignature = ClassSignature.readFrom(classNode.signature, mapOf())
        testClass(classSignature, classNode.signature)

        val classTypeArgMap = classSignature.typeArguments?.map { it.name to it }?.toMap() ?: mapOf()

        val method1Sig = classNode.methods[1].signature
        val method2Sig = classNode.methods[2].signature
        val method1Signature = MethodSignature.readFrom(method1Sig, classTypeArgMap)
        val method2Signature = MethodSignature.readFrom(method2Sig, classTypeArgMap)

        testMethod(method1Signature, method1Sig, classTypeArgMap)
        testMethod(method2Signature, method2Sig, classTypeArgMap)

        val field1Signature = FieldSignature.readFrom(classNode.fields[0].signature, classTypeArgMap)
        val field2Signature = FieldSignature.readFrom(classNode.fields[1].signature, classTypeArgMap)
        val x = 2
    }

    private fun testClass(classSignature: ClassSignature, signature: String) {
        val asString = classSignature.toClassfileName()
        val backToClass = ClassSignature.readFrom(asString, mapOf())
        val asStringAgain = backToClass.toClassfileName()

        assertEquals(classSignature.toString(), backToClass.toString())
        assertEquals(signature, asString)
        assertEquals(asString, asStringAgain)
    }
//expected: metautils.signature.ClassSignature@a11e3696<<T extends GenericsTest$InnerClass implements Comparable<String>, List<T>, U extends T> (extends Object)> but was: metautils.signature.ClassSignature@146a42b1<<T extends GenericsTest$InnerClass implements Comparable<String>, List<T>, U extends T> (extends Object)>

    private fun testMethod(methodSignature: MethodSignature, signature: String, args: TypeArgDecls) {
        val asString = methodSignature.toClassfileName()
        val backToMethod = MethodSignature.readFrom(asString, args)
        val asStringAgain = backToMethod.toClassfileName()

        assertEquals(methodSignature.toString(), backToMethod.toString())
        assertEquals(signature, asString)
        assertEquals(asString, asStringAgain)
    }

}