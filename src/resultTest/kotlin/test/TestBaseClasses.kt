package test

import net.minecraft.TestAbstractImpl
import net.minecraft.TestConcreteClass
import org.junit.jupiter.api.Test
import v1.net.minecraft.*

class X : BaseTestAbstractImpl(0,null) {

}

@Suppress("CAST_NEVER_SUCCEEDS")
class TestBaseClasses {
    private fun <T> assertEquals(actual: T, expected: T) = kotlin.test.assertEquals(expected, actual)

//    public abstract class TestAbstractClass {
//    public int field;
//
//    public abstract TestAbstractClass abstractMethod();
//    public abstract TestAbstractClass abstractMethodParam(TestConcreteClass x);
//
//    public TestAbstractClass(int x1, TestAbstractClass x2){}
//}

//public class TestAbstractImpl extends TestAbstractClass implements TestInterface, Comparable<TestAbstractImpl> {
//    public TestAbstractImpl(int x1, TestAbstractClass x2) {
//        super(x1, x2);
//    }
//
//    public TestAbstractClass abstractMethod() {
//        return new TestAbstractImpl(1, (TestAbstractClass)null);
//    }
//
//    public TestAbstractClass abstractMethodParam(TestConcreteClass x) {
//        return new TestAbstractImpl(2, new TestAbstractImpl(3, (TestAbstractClass)null));
//    }
//
//    public TestInterface foo() {
//        return null;
//    }
//
//    public TestOtherClass boz(TestOtherClass x) {
//        return null;
//    }
//
//    public int compareTo(TestAbstractImpl o) {
//        return 0;
//    }
//}

//    class X : ITestAbstractImpl


    //TODO: there is indeed infinite recursion when it's not being overriden

    @Test
    fun testAbstractImpl() {
        println("Running testAbstractImpl baseclass test")

        with(object : BaseTestAbstractImpl(0, ITestAbstractImpl.create(0, null)) {
        }) {
            abstractMethod()
        }

        val expectedObj1 = ITestAbstractImpl.create(0, null)
        val expectedObj2 = ITestAbstractImpl.create(0, null)

        with(object : BaseTestAbstractImpl(0, ITestAbstractImpl.create(0, null)) {
            override fun abstractMethod(): ITestAbstractClass {
                return expectedObj1
            }

            override fun abstractMethodParam(p0: ITestConcreteClass): ITestAbstractClass? {
                return expectedObj2
            }
        }) {
            testAbstractImplCalls()
        }

        val expectedParam = TestConcreteClass()

        with(object : BaseTestAbstractImpl(0, ITestAbstractImpl.create(0, null)) {
            override fun abstractMethod(): ITestAbstractClass {
                return expectedObj1
            }

            override fun abstractMethodParam(p0: ITestConcreteClass): ITestAbstractClass? {
                assertEquals(p0, expectedParam)
                return expectedObj2
            }
        }) {
            val mcThis = this as TestAbstractImpl
            assertEquals(expectedObj1, mcThis.abstractMethod())
            assertEquals(expectedObj2, mcThis.abstractMethodParam(expectedParam))
        }


    }

    @Test
    fun testClashingNames() {
        with(ITestClashingNames.create()) {
            assertEquals(isSomeBool_field, false)
            isSomeBool = true
            assertEquals(isSomeBool_field, true)
            assertEquals(isSomeBool, false)
            assertEquals(someInt, 0)
            someInt = 2
            assertEquals(someInt, 2)
            assertEquals(getSomeInt(3), 0)
        }
    }

    @Test
    fun testConcreteClass() {
        with(object : BaseTestConcreteClass(0, ITestOtherClass.create()) {
            override fun publicInt(p0: ITestOtherClass?): Int {
                return 3
            }
        }) {
            testConcreteClassCalls()
            val mcThis = this as TestConcreteClass
            assertEquals(mcThis.publicInt(null), 3)
        }

        with(object : BaseTestConcreteClass.TestStaticInnerClass(1, ITestOtherClass.create()) {

        }) {
            assertEquals(publicInt(), 2)
        }

    }


    @Test
    fun testFinalClass() {
        assertEquals(ITestFinalClass.getPublicStaticField(), null)
        ITestFinalClass.setPublicStaticField("bar")
        assertEquals(ITestFinalClass.getPublicStaticField(), "bar")
        assert(ITestFinalClass.publicStaticOtherClassField is ITestOtherClass)
        with(ITestFinalClass.create(ITestOtherClass.create())) {
            assertEquals(inheritedMethod(), 2)
            assertEquals(overridenMethod(), 3)
            assertEquals(inheritedField, "inherited")
            inheritedField = "foo"
            assertEquals(inheritedField, "foo")

            assertEquals(publicField, null)
            assertEquals(publicFinalField, 2)
            assertEquals(publicInt(ITestOtherClass.create()), 2)
            assertEquals(publicFinalInt(), 2)
        }
    }

    @Test
    fun testInnerExtender() {
        with(ITestInnerExtender.create(0, ITestOtherClass.create())) {
            assertEquals(publicInt(), 2)
            assertEquals(inheritedMethod(), 2)
            normalMethod()
        }
    }

    @Test
    fun testInterface() {
        assertEquals(ITestInterface.x, 2)
    }

    @Test
    fun testOtherClass() {
        with(ITestOtherClass.create()) {
            oneCastTest(ITestConcreteClass.create(0, null))
            twoCastTest(ITestConcreteClass.create(0, null))
            realFinalCastTest(ITestFinalClass.create(ITestOtherClass.create()))
        }
    }

//    @Test
//    fun testOverrideReturnTypeChange() {
//        with(ITestOverrideReturnTypeChange.create()) {
//            val x: List<*>? = foo()
//            assertEquals(x, null)
//            val y: ArrayList<ITestOtherClass>? = bar()
//            assertEquals(y, null)
//            val z: ITestAbstractImpl? = mcClass()
//            assertEquals(z, null)
//        }
//    }
//
//    @Test
//    fun testOverrideReturnTypeChangeSuper() {
//        with(ITestOverrideReturnTypeChangeSuper.create()) {
//            val x: Any? = foo()
//            assertEquals(x, null)
//            val y: List<ITestOtherClass>? = bar()
//            assertEquals(y, null)
//            val z: ITestAbstractClass? = mcClass()
//            assertEquals(z, null)
//        }
//    }
//
//    @Test
//    fun testArrays() {
//        with(ITestArrays.create()) {
//            assertEquals(arrField, null)
//            arrField = ITestConcreteClass.array(5)
//            arrField[2] = ITestConcreteClass.create()
//            arrField[3] = ITestConcreteClass.create()
//
//            val x: ITestConcreteClass = arrField[2]
//            val y: ITestConcreteClass = arrField[3]
//
//            assert(x is ITestConcreteClass)
//            assert(y is ITestConcreteClass)
//            assertEquals(arrField[1], null)
//
//            val arrFromMethod = arrMethod()
//            assert(arrFromMethod[1] is ITestConcreteClass)
//            arrFromMethod[0] = ITestConcreteClass.create()
//
//            arrParam(arrField)
//            arrParam(arrFromMethod)
//            arrParam(arrMethod())
//        }
//    }
//
//    @Test
//    fun testEnum(){
//        assertEquals(ITestEnum.foo(), ITestEnum.THING2)
//        with(ITestEnum.THING) {
//            assertEquals(bar(), null)
//            assertEquals(x, 1)
//            x = 3
//            assertEquals(x, 3)
//        }
//
//        with(ITestEnum.THING2 as Enum<*>){
//            assertEquals(name, "THING2")
//            assertEquals(ordinal, 1)
//        }
//
//        with(ITestEnum.values()){
//            assertEquals(size, 2)
//            assertEquals(this[0], ITestEnum.THING)
//            assertEquals(this[1], ITestEnum.THING2)
//        }
//
//        assertEquals(ITestEnum.valueOf("THING"), ITestEnum.THING)
//    }
//
//    @Test
//    fun testGenerics() {
//        with(ITestGenerics.create<ArrayList<ITestConcreteClass>, ArrayList<ITestConcreteClass>,
//                List<ArrayList<ITestConcreteClass>>, Int>()){
//           val x : ITestInterface = ITestAbstractImpl.create(0, null)
//            val y : ArrayList<ITestConcreteClass>? = genericMethod<ArrayList<ITestConcreteClass>>(
//                ArrayList(),
//                ArrayList(),
//                listOf(),
//                listOf(),
//                listOf(ITestAbstractImpl.create(0, null)),
//                mutableListOf(x),
//                listOf(1,2,"3")
//            )
//
//            genericField1 = ArrayList()
//            genericField1.add(ITestConcreteClass.create())
//            assert(genericField1[0] is ITestConcreteClass)
//
//            newSomeInnerClass<Int>()
//        }
//
//        ITestGenerics.Extendor.create<ArrayList<ITestConcreteClass>>()
//
//
//        ITestGenerics.SomeInnerClass.array<ArrayList<ITestConcreteClass>, ArrayList<ITestConcreteClass>,
//                List<ArrayList<ITestConcreteClass>>, Int,Int>(5)
//    }

}
