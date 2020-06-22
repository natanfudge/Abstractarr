package test

import net.minecraft.TestAbstractImpl
import net.minecraft.TestConcreteClass
import net.minecraft.TestProtected
import org.junit.jupiter.api.Test
import v1.net.minecraft.*


@Suppress("CAST_NEVER_SUCCEEDS")
class TestBaseClasses {
    private fun <T> assertEquals(actual: T, expected: T) = kotlin.test.assertEquals(expected, actual)


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

        val ep1 = TestConcreteClass()
        val ep2 = ITestOtherClass.create()

        val e3 = ITestAbstractImpl.create(0,null)
        val e4 = ITestOtherClass.create()
        val e5 = ITestOtherClass.create()
        val e6 = ITestOtherClass.create()
        val ep3 = ITestOtherClass.create()

        with(object : BaseTestAbstractImpl(0, ITestAbstractImpl.create(0, null)) {
            override fun abstractMethod(): ITestAbstractClass {
                return expectedObj1
            }

            override fun abstractMethodParam(p0: ITestConcreteClass): ITestAbstractClass? {
                assertEquals(p0, ep1)
                return expectedObj2
            }

            override fun foo(): ITestInterface {
                return e3
            }

            override fun baz(): ITestOtherClass {
                return e4
            }

            override fun baz(p0: ITestOtherClass?): ITestOtherClass {
                assertEquals(ep2, p0)
                return e5
            }

            override fun boz(p0: ITestOtherClass?): ITestOtherClass {
                return e6
            }

            override fun someImplMethodWithArg(p0: ITestOtherClass?) {
                assertEquals(p0,ep3)
                super.someImplMethodWithArg(p0)
            }

            override fun compareTo(other: ITestAbstractImpl?): Int {
                return 4
            }
        }) {
            this as TestAbstractImpl
            assertEquals(expectedObj1, abstractMethod())
            assertEquals(expectedObj2, abstractMethodParam(ep1))
            assertEquals(e3,foo())
            assertEquals(e4,baz())
            assertEquals(e5,baz(ep2))
            assertEquals(e6,boz(ep2))
            someImplMethodWithArg(ep3)
            assertEquals(compareTo(ITestAbstractImpl.create(0,null)), 4)
        }
    }

    @Test
    fun testClashingNames() {
        object: BaseTestClashingNames() {
        }.testClashingNamesCalls()

        with(object: BaseTestClashingNames(){
            override fun isSomeBool(): Boolean {
                return true
            }

            override fun getSomeString(): String {
                return "replaced"
            }

            override fun getSomeInt(p0: Int): Int {
                return 10
            }
        }){
            assertEquals(isSomeBool, true)
            assertEquals(someString, "replaced")
            assertEquals(getSomeInt(1), 10)
        }
    }

    @Test
    fun testConcreteClass() {
        with(object : BaseTestConcreteClass(0, ITestOtherClass.create()) {
        }) {
            testConcreteClassCalls()
        }

        with(object : BaseTestConcreteClass(0, ITestOtherClass.create()) {
            override fun publicInt(p0: ITestOtherClass?): Int {
                return 3
            }



        }) {
            val mcThis = this as TestConcreteClass
            assertEquals(mcThis.publicInt(null), 3)
        }

        with(object : BaseTestConcreteClass.TestStaticInnerClass(1, ITestOtherClass.create()) {

        }) {
            assertEquals(publicInt(), 2)
        }

//        object  : TestProtected(){
//            override fun fooNoMc() {
//                val x = TestProtected.constant
//                staticField = "bar"
//            }
//        }
    }

//
//    protected static String protectedStatic() {
//        return "SomeString";
//    }
//
//    public static int publicStatic() {
//        return 4;
//    }
//
//    int packageArgs(TestOtherClass arg1, int arg2) {
//        return 1;
//    }
//
//    public int mutatesField() {
//        privateField++;
//        return 123;
//    }
//
//    public final int finalMethod() {
//        return 3;
//    }
//
//    public TestStaticInnerClass innerClassMethod() {
//        return new TestStaticInnerClass(23, new TestOtherClass());
//    }
//
//    public TestConcreteClass(int arg1, TestOtherClass arg2) {
//        super(arg2);
//    }
//
//    public TestConcreteClass() {
//        this(0, null);
//    }


    @Test
    fun testFinalClass() {
        assertEquals(ITestFinalClass.getPublicStaticField(), null)
        ITestFinalClass.setPublicStaticField("bar")
        assertEquals(ITestFinalClass.getPublicStaticField(), "bar")
        ITestFinalClass.setPublicStaticField(null)
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
