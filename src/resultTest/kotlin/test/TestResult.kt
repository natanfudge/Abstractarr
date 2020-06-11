package test

import org.junit.jupiter.api.Test
import v1.net.minecraft.*

class TestResult {
    private fun <T> assertEquals(actual: T, expected: T) = kotlin.test.assertEquals(expected, actual)

    @Test
    fun testAbstractImpl() {
        with(ITestAbstractImpl.create(0, null)) {
            assert(abstractMethod() is ITestAbstractClass)
            assert(abstractMethodParam(ITestConcreteClass.create(0, null)) is ITestAbstractClass)
            assertEquals(field, 0)
            field = 2
            assertEquals(field, 2)
            assertEquals(foo(), null)
            assertEquals(bar(), 2)
            assertEquals(compareTo(ITestAbstractImpl.create(0, null)), 0)
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

        assert(ITestConcreteClass.publicStaticOtherClassField is ITestOtherClass)
        assertEquals(ITestConcreteClass.publicStaticFinalField, "BAR")
        assertEquals(ITestConcreteClass.publicStatic(), 4)
        assert(ITestConcreteClass.create(1, ITestOtherClass.create()) is ITestConcreteClass)
        assertEquals(ITestConcreteClass.getPublicStaticField(), null)
        ITestConcreteClass.setPublicStaticField("foo")
        assertEquals(ITestConcreteClass.getPublicStaticField(), "foo")
        assertEquals(ITestConcreteClass.TestInnerClass.publicStaticFinalField, "BAR")
        assertEquals(ITestConcreteClass.TestStaticInnerClass.publicStaticFinalField, "BAR")

        assert(ITestConcreteClass.TestStaticInnerClass.publicStaticOtherClassField is ITestOtherClass?)
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

        with(ITestConcreteClass.TestStaticInnerClass.create(1, ITestOtherClass.create())) {
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

    @Test
    fun testOverrideReturnTypeChange() {
        with(ITestOverrideReturnTypeChange.create()) {
            val x: List<*>? = foo()
            assertEquals(x, null)
            val y: ArrayList<ITestOtherClass>? = bar()
            assertEquals(y, null)
            val z: ITestAbstractImpl? = mcClass()
            assertEquals(z, null)
        }
    }

    @Test
    fun testOverrideReturnTypeChangeSuper() {
        with(ITestOverrideReturnTypeChangeSuper.create()) {
            val x: Any? = foo()
            assertEquals(x, null)
            val y: List<ITestOtherClass>? = bar()
            assertEquals(y, null)
            val z: ITestAbstractClass? = mcClass()
            assertEquals(z, null)
        }
    }

    @Test
    fun testArrays() {
        with(ITestArrays.create()) {
            assertEquals(arrField, null)
            arrField = ITestConcreteClass.array(5)
            arrField[2] = ITestConcreteClass.create()
            arrField[3] = ITestConcreteClass.create()

            val x: ITestConcreteClass = arrField[2]
            val y: ITestConcreteClass = arrField[3]

            assert(x is ITestConcreteClass)
            assert(y is ITestConcreteClass)
            assertEquals(arrField[1], null)

            val arrFromMethod = arrMethod()
            assert(arrFromMethod[1] is ITestConcreteClass)
            arrFromMethod[0] = ITestConcreteClass.create()

            arrParam(arrField)
            arrParam(arrFromMethod)
            arrParam(arrMethod())
        }
    }

    @Test
    fun testEnum(){
        assertEquals(ITestEnum.foo(), ITestEnum.THING2)
        with(ITestEnum.THING) {
            assertEquals(bar(), null)
            assertEquals(x, 1)
            x = 3
            assertEquals(x, 3)
        }

        with(ITestEnum.THING2 as Enum<*>){
            assertEquals(name, "THING2")
            assertEquals(ordinal, 1)
        }

        with(ITestEnum.values()){
            assertEquals(size, 2)
            assertEquals(this[0], ITestEnum.THING)
            assertEquals(this[1], ITestEnum.THING2)
        }

        assertEquals(ITestEnum.valueOf("THING"), ITestEnum.THING)
    }

    @Test
    fun testGenerics() {
//        with(ITestGenerics.create())
    }

//    public class GenericsTest<T1 extends ArrayList<TestConcreteClass>,
//        T2 extends T1, T3 extends List<T2>, T4 extends List<Boolean> & Comparable<String>> extends ArrayList<T3> implements List<T3>,
//        Comparable<TestInterface>, ExtendedInterface<TestAbstractImpl>, ExtendedInterfaceRaw {
//    @Override
//    public int compareTo(TestInterface o) {
//        return 2;
//    }
//
//    public static class Extendor<T extends ArrayList<TestConcreteClass>>
//            extends GenericsTest<T, T, List<T>, SomethingToInsert> {
//    }
//
//    public abstract static class SomethingToInsert  implements Comparable<String>, List<Boolean>{
//
//        @Override
//        public int compareTo(String o) {
//            return 0;
//        }
//    }
//
//    public <T extends T2> T genericMethod(T param1, T2 param2, List<String> param3, List<TestAbstractClass> param4,
//                                          List<? extends TestAbstractClass> param5, List<? super TestAbstractImpl> param6, List<?> param7) {
//        return null;
//    }
//
//    public class SomeInnerClass<T>{
//
//    }
//
//    public T1 genericField1;
//    public List<?> genericField2;
//    public List<? extends T2> genericField3;
//    public List<? super T1> genericField4;
//    public List<TestInterface> genericField5;
//}
}
