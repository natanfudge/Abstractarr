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
        val classSignature = ClassSignature.fromSignatureString(classNode.signature, listOf())
        testClass(classSignature, classNode.signature)

        val classTypeArgs = classSignature.typeArguments

        val method1Sig = classNode.methods[1].signature
        val method2Sig = classNode.methods[2].signature
        val method1Signature = MethodSignature.fromSignatureString(method1Sig, classTypeArgs)
        val method2Signature = MethodSignature.fromSignatureString(method2Sig, classTypeArgs)

        testMethod(method1Signature, method1Sig, classTypeArgs)
        testMethod(method2Signature, method2Sig, classTypeArgs)

        val field1Signature = FieldSignature.fromFieldSignature(classNode.fields[0].signature, classTypeArgs)
        val field2Signature = FieldSignature.fromFieldSignature(classNode.fields[1].signature, classTypeArgs)
        val x = 2
    }

    private fun testClass(classSignature: ClassSignature, signature: String) {
        val asString = classSignature.toClassfileName()
        val backToClass = ClassSignature.fromSignatureString(asString, listOf())
        val asStringAgain = backToClass.toClassfileName()

        assertEquals(classSignature.toString(), backToClass.toString())
        assertEquals(signature, asString)
        assertEquals(asString, asStringAgain)
    }
//expected: metautils.signature.ClassSignature@a11e3696<<T extends GenericsTest$InnerClass implements Comparable<String>, List<T>, U extends T> (extends Object)> but was: metautils.signature.ClassSignature@146a42b1<<T extends GenericsTest$InnerClass implements Comparable<String>, List<T>, U extends T> (extends Object)>

    private fun testMethod(methodSignature: MethodSignature, signature: String, args: Iterable<TypeArgumentDeclaration>) {
        val asString = methodSignature.toClassfileName()
        val backToMethod = MethodSignature.fromSignatureString(asString, args)
        val asStringAgain = backToMethod.toClassfileName()

        assertEquals(methodSignature.toString(), backToMethod.toString())
        assertEquals(signature, asString)
        assertEquals(asString, asStringAgain)
    }

}