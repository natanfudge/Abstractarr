package test

import net.minecraft.*
import org.junit.jupiter.api.Test
import testBaseclass
import testBaseclassExtendor
import v1.net.minecraft.*


@Suppress("CAST_NEVER_SUCCEEDS")
class TestBaseClasses {
    private fun <T> assertEquals(actual: T, expected: T) = kotlin.test.assertEquals(expected, actual)


    @Test
    fun testAbstractClass(){
        //TODO
//        object: BaseTestAbstractClass(0, null){
//            override fun abstractMethod(): ITestAbstractClass {
//                TODO("Not yet implemented")
//            }
//
//            override fun abstractMethodParam(p0: ITestConcreteClass?): ITestAbstractClass {
//                TODO("Not yet implemented")
//            }
//
//            override fun boz(p0: ITestOtherClass?): ITestOtherClass {
//                TODO("Not yet implemented")
//            }
//
//            override fun foo(): ITestInterface {
//                TODO("Not yet implemented")
//            }
//        }
    }

    @Test
    fun testAbstractImpl() {
        println("Running testAbstractImpl baseclass test")

        object : BaseTestAbstractImpl(0, ITestAbstractImpl.create(0, null)) {
        }.apply {
            abstractMethod()
        }

        val expectedObj1 = ITestAbstractImpl.create(0, null)
        val expectedObj2 = ITestAbstractImpl.create(0, null)

        object : BaseTestAbstractImpl(0, ITestAbstractImpl.create(0, null)) {
            override fun abstractMethod(): ITestAbstractClass {
                return expectedObj1
            }

            override fun abstractMethodParam(p0: ITestConcreteClass): ITestAbstractClass? {
                return expectedObj2
            }
        }.apply {
            testAbstractImplCalls()
        }

        val ep1 = TestConcreteClass()
        val ep2 = ITestOtherClass.create()

        val e3 = ITestAbstractImpl.create(0, null)
        val e4 = ITestOtherClass.create()
        val e5 = ITestOtherClass.create()
        val e6 = ITestOtherClass.create()
        val ep3 = ITestOtherClass.create()

        object : BaseTestAbstractImpl(0, ITestAbstractImpl.create(0, null)) {
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
                assertEquals(p0, ep3)
                super.someImplMethodWithArg(p0)
            }

            override fun compareTo(other: ITestAbstractImpl?): Int {
                return 4
            }
        }.apply {
            this as TestAbstractImpl
            assertEquals(expectedObj1, abstractMethod())
            assertEquals(expectedObj2, abstractMethodParam(ep1))
            assertEquals(e3, foo())
            assertEquals(e4, baz())
            assertEquals(e5, baz(ep2))
            assertEquals(e6, boz(ep2))
            someImplMethodWithArg(ep3)
            assertEquals(compareTo(ITestAbstractImpl.create(0, null)), 4)
        }
    }


    @Test
    fun testFinalClass() {
//        val x = object: BaseTestFinalClass
    }


    @Test
    fun testAnnotations() {
        object : BaseTestAnnotations() {
        }.apply {
            testAnnotationsCalls()
        }

        val expected = ITestAbstractImpl.create(0, null)
        object : BaseTestAnnotations() {
            override fun abstractMethod(): ITestAbstractClass? {
                return expected
            }

            override fun abstractMethodParam(p0: ITestConcreteClass?): ITestAbstractClass {
                return super.abstractMethodParam(p0)
            }

            override fun nullable(): String? {
                return "foonull"
            }

            override fun foo(): ITestInterface {
                return super.foo()
            }

            override fun boz(p0: ITestOtherClass?): ITestOtherClass {
                return super.boz(p0)
            }


        }.apply {
            this as TestAnnotations
            assertEquals(nullable(), "foonull")
            assertEquals(expected, abstractMethod())
        }
    }

    @Test
    fun testArrays() {
        object : BaseTestArrays() {
        }.apply {
            testArraysCalls()
        }

        val expected = ITestConcreteClass.create()
        val eParam = arrayOfNulls<TestConcreteClass>(3)
        object : BaseTestArrays() {
            override fun arrMethod(): Array<ITestConcreteClass> {
                val arr = ITestConcreteClass.array(3)
                arr[0] = expected
                return arr
            }

            override fun arrParam(p0: Array<out ITestConcreteClass>?) {
                assertEquals(eParam, p0)
            }
        }.apply {
            val x = this as TestArrays
            val y = x.arrMethod()[0]
            assertEquals(y, expected)
            arrParam(eParam)
        }
    }

    @Test
    fun testClashingNames() {
        object : BaseTestClashingNames() {
        }.testClashingNamesCalls()

        with(object : BaseTestClashingNames() {
            override fun isSomeBool(): Boolean {
                return true
            }

            override fun getSomeString(): String {
                return "replaced"
            }

            override fun getSomeInt(p0: Int): Int {
                return 10
            }
        }) {
            val mcThis = this as TestClashingNames
            assertEquals(mcThis.isSomeBool, true)
            assertEquals(mcThis.getSomeString(), "replaced")
            assertEquals(mcThis.getSomeInt(1), 10)
        }
    }


    @Test
    fun testConcreteClass() {
        object : BaseTestConcreteClass(0, ITestOtherClass.create()) {
        }.apply {
            testConcreteClassCalls()

        }

        val expected = ITestConcreteClass.TestStaticInnerClass.create(0, ITestOtherClass.create())

        object : BaseTestConcreteClass(0, ITestOtherClass.create()) {
            override fun publicInt(p0: ITestOtherClass?): Int {
                return 3
            }

            override fun mutatesField(): Int {
                return 100
            }

            override fun innerClassMethod(): ITestConcreteClass.TestStaticInnerClass {
                return expected
            }


        }.apply {
            val mcThis = this as TestConcreteClass
            assertEquals(mcThis.publicInt(null), 3)
            assertEquals(mutatesField(), 100)
            assertEquals(innerClassMethod(), expected)
        }


        object : BaseTestConcreteClass.TestStaticInnerClass(1, ITestOtherClass.create()) {
            override fun publicInt(): Int {
                return 50
            }

            override fun mutatesField(): Int {
                return 100
            }

        }.apply {
            val mcThis = this as TestConcreteClass.TestStaticInnerClass
            assertEquals(publicInt(), 50)
            assertEquals(mcThis.publicInt(), 50)
            assertEquals(mutatesField(), 100)
        }

    }


    @Test
    fun testGenerics() {
        with(
            object : BaseTestGenerics<ArrayList<ITestConcreteClass>, ArrayList<ITestConcreteClass>,
                    List<ArrayList<ITestConcreteClass>>, Int>() {

            }
        ) {
            testGenericsCalls()

        }


        val eParam = listOf<String>()

        object : BaseTestGenerics<ArrayList<ITestConcreteClass>, ArrayList<ITestConcreteClass>,
                List<ArrayList<ITestConcreteClass>>, Int>() {
            override fun compareTo(other: ITestInterface?): Int {
                return 23
            }

            override fun <T : ArrayList<ITestConcreteClass>?> genericMethod(
                p0: T,
                p1: ArrayList<ITestConcreteClass>?,
                p2: MutableList<String>?,
                p3: MutableList<ITestAbstractClass>?,
                p4: MutableList<out ITestAbstractClass>?,
                p5: MutableList<in ITestAbstractImpl>?,
                p6: MutableList<*>?,
                p7: ITestOtherClass?
            ): T? {
                assertEquals(p2, eParam)
                return null
            }
        }.apply {
            val x = this as TestGenerics<ArrayList<TestConcreteClass>, ArrayList<TestConcreteClass>,
                    List<ArrayList<TestConcreteClass>>, Int>
            assertEquals(x.compareTo(null), 23)

            testBaseclass(eParam)
        }

        val expectedParam = TestOtherClass()
        object : BaseTestGenerics.Extendor<ArrayList<ITestConcreteClass>>() {
            override fun <T_OVERRIDE : ArrayList<ITestConcreteClass>?> genericMethod(
                p0: T_OVERRIDE,
                p1: ArrayList<ITestConcreteClass>?,
                p2: MutableList<String>?,
                p3: MutableList<ITestAbstractClass>?,
                p4: MutableList<out ITestAbstractClass>?,
                p5: MutableList<in ITestAbstractImpl>?,
                p6: MutableList<*>?,
                p7: ITestOtherClass?
            ): T_OVERRIDE {
                assertEquals(p7, expectedParam)
                return super.genericMethod(p0, p1, p2, p3, p4, p5, p6, p7)
            }
        }.apply {
            val mcThis = this as TestGenerics.Extendor<ArrayList<TestConcreteClass>>
            mcThis.testBaseclassExtendor(expectedParam)
        }
    }

    @Test
    fun testInnerExtender() {
        object : BaseTestInnerExtender(0, ITestOtherClass.create()) {

        }.apply {
            testInnerExtenderCalls()
        }

        var x = 0

        object : BaseTestInnerExtender(0, ITestOtherClass.create()) {
            init {
                assertEquals(protectedStatic(), "SomeString")
            }
            override fun inheritedMethod(): Int {
                return 5
            }

            override fun overridenMethod(): Int {
                return 6
            }

            override fun publicInt(): Int {
                return 7
            }

            override fun mutatesField(): Int {
                return 8
            }

            override fun normalMethod() {
                x = 9
            }

        }.apply {
            assertEquals(inheritedField,"inherited")
            assertEquals(finalMethod(), 3)
            this as TestInnerExtender
            assertEquals(inheritedMethod(),5)
            assertEquals(overridenMethod(), 6)

            assertEquals(ITestConcreteClass.TestStaticInnerClass.publicStatic(), 4)
            assertEquals(publicInt(),7)
            assertEquals(mutatesField(),8)
            assertEquals(finalMethod(),3)
            normalMethod()
            assertEquals(x, 9)

        }
    }



    @Test
    fun testInterface() {
        //TODO
//        object : BaseTestInterface {
//
//        }.apply {
//
//        }
    }

//    public interface TestInterface {
//    TestInterface foo();
//
//    default int bar() {
//        return 2;
//    }
//
//    default TestOtherClass baz() {
//        return null;
//    }
//
//    default TestOtherClass baz(TestOtherClass x) {
//        return null;
//    }
//
//    TestOtherClass boz(TestOtherClass x);
//
//    int x = 2;
//
//    public static void testStatic(){
//
//    }
//
//    public static void testStaticParam(TestOtherClass mc) {
//
//    }
//}

    //TODO: make sure you can't make lambdas of interfaces willy nilly (only baseclass)
    @Test
    fun testLambdaInterface() {
        //TODO
//        object : BaseTestLambdaInterface {
//
//        }.apply {
//
//        }
    }


    @Test
    fun testNormalClassExtender() {
        //TODO
        object : BaseTestNormalClassExtender() {

        }.apply {
            testNormalClassExtenderCalls()
        }

        object : BaseTestNormalClassExtender() {

        }.apply {
        }
    }

    @Test
    fun testOverload() {
        //TODO
        object : BaseTestOverload() {

        }.apply {
            testOverloadCalls()
        }

        object : BaseTestOverload() {

        }.apply {
        }
    }

    @Test
    fun testOtherClass() {
        //TODO
        object : BaseTestOtherClass() {

        }.apply {
            testOtherClassCalls()
        }

        object : BaseTestOtherClass() {

        }.apply {
        }

    }

    @Test
    fun testOverrideReturnTypeChange() {
        //TODO
        object : BaseTestOverrideReturnTypeChange() {

        }.apply {
            testOverrideReturnTypeChangeCalls()
        }

        object : BaseTestOverrideReturnTypeChange() {

        }.apply {
        }

    }

    @Test
    fun testOverrideReturnTypeChangeSuper() {
        //TODO
        object : TestOverrideReturnTypeChangeSuper() {

        }.apply {
            testOverrideReturnTypeChangeSuperCalls()
        }

        object : TestOverrideReturnTypeChangeSuper() {

        }.apply {
        }
    }


    @Test
    fun testSuperClass() {
        //TODO
        object : BaseTestSuperClass(null) {

        }.apply {

        }
    }

    @Test
    fun testThrows() {
        try {
            //TODO: (needs to have throws in baseclasses...)
            object : BaseTestThrows() {

            }.apply {
                testThrowsCalls()
            }

            object : BaseTestThrows() {

            }.apply {
            }

        } catch (e: Exception) {

        }

    }


}
