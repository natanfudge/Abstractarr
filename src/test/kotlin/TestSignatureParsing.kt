import asm.readToClassNode
import metautils.signature.*
import org.junit.jupiter.api.Test
import signature.*
import testing.getResource
import kotlin.test.assertEquals

@Suppress("UNUSED_VARIABLE")
class TestSignatureParsing {
    @Test
    fun testSignatures() {
        val classNode = readToClassNode(getResource("GenericsTest.class"))
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

        assertEquals(classSignature, backToClass)
        assertEquals(signature, asString)
        assertEquals(asString, asStringAgain)
    }


    private fun testMethod(methodSignature: MethodSignature, signature: String, args: TypeArgDecls) {
        val asString = methodSignature.toClassfileName()
        val backToClass = MethodSignature.readFrom(asString, args)
        val asStringAgain = backToClass.toClassfileName()

        assertEquals(methodSignature, backToClass)
        assertEquals(signature, asString)
        assertEquals(asString, asStringAgain)
    }

}