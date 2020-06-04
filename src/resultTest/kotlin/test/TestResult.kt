package test

import net.minecraft.TestConcreteClass
import net.minecraft.TestOtherClass
import org.junit.jupiter.api.Test
import v1.net.minecraft.ITestConcreteClass
import v1.net.minecraft.ITestOtherClass
import kotlin.test.assertEquals

class TestResult {
    //TODO: create new project for runtime asm (interface attaching), and use it to transform the testOriginalJar after build.
    @Test
    fun testConcreteClass() {

        assert(ITestConcreteClass.publicStaticOtherClassField is ITestOtherClass)
        assertEquals(ITestConcreteClass.publicStaticFinalField, "BAR")
        assertEquals(ITestConcreteClass.publicStatic(), 4)
        assert(ITestConcreteClass.create(1, ITestOtherClass.create()) is ITestConcreteClass)
        assertEquals(ITestConcreteClass.getPublicStaticField(), null)
        ITestConcreteClass.setPublicStaticField("foo")
        assertEquals(ITestConcreteClass.getPublicStaticField(), "foo")
        assertEquals(ITestConcreteClass.TestInnerClass.publicStaticFinalField, "BAR")
        assertEquals(ITestConcreteClass.TestStaticInnerClass.publicStaticFinalField, "BAR")
        assert(ITestConcreteClass.TestStaticInnerClass.publicStaticOtherClassField is ITestOtherClass)
        assertEquals(ITestConcreteClass.TestStaticInnerClass.publicStatic(), 4)

        with(ITestConcreteClass.create(0, ITestOtherClass.create())) {
            assertEquals(publicInt(), 2)
            assertEquals(mutatesField(), 123)
            assertEquals(finalMethod(), 3)
            assert(innerClassMethod() is ITestConcreteClass.TestStaticInnerClass)
            assertEquals(publicField, 0)
            publicField = 3
            assertEquals(publicField, 3)
            assertEquals(publicFinalField, 2)
            val otherClassFieldVar = otherClassField
            assert(otherClassFieldVar is ITestOtherClass)
            otherClassField = ITestOtherClass.create()
            assert(otherClassField !== otherClassFieldVar)
        }

        with(ITestConcreteClass.create(0, ITestOtherClass.create()).newTestInnerClass(123, ITestOtherClass.create())) {
            assertEquals(publicInt(), 2)
            assertEquals(publicField, 0)
            assertEquals(mutatesField(), 123)
            assertEquals(publicField, 1)
            assertEquals(finalMethod(), 3)
            publicField = 69
            assertEquals(publicField, 69)
            assertEquals(publicFinalField, 2)
            val otherClassFieldVar = otherClassField
            assert(otherClassFieldVar is ITestOtherClass)
            otherClassField = ITestOtherClass.create()
            assert(otherClassField !== otherClassFieldVar)
        }

        with(ITestConcreteClass.TestStaticInnerClass.create(1, ITestOtherClass.create())){
            assertEquals(publicInt(), 2)
        }

    }



//

}