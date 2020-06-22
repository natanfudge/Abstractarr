package test

import net.minecraft.*
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

        val e3 = ITestAbstractImpl.create(0, null)
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
                assertEquals(p0, ep3)
                super.someImplMethodWithArg(p0)
            }

            override fun compareTo(other: ITestAbstractImpl?): Int {
                return 4
            }
        }) {
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

            with(object : BaseTestConcreteClass.TestInnerClass(null) {

            }) {
                testConcreteInnerCalls()
            }

            with(object : BaseTestConcreteClass.TestInnerClass(null) {
                //TODO
            }) {
            }
        }

        with(object : BaseTestConcreteClass(0, ITestOtherClass.create()) {
            override fun publicInt(p0: ITestOtherClass?): Int {
                return 3
            }


        }) {
            val mcThis = this as TestConcreteClass
            assertEquals(mcThis.publicInt(null), 3)
        }

        //TODO
        with(object : BaseTestConcreteClass.TestStaticInnerClass(1, ITestOtherClass.create()) {

        }) {
            assertEquals(publicInt(), 2)
        }


    }


    @Test
    fun testGenerics() {
        //TODO
        with(
            object : BaseTestGenerics<ArrayList<ITestConcreteClass>, ArrayList<ITestConcreteClass>,
                    List<ArrayList<ITestConcreteClass>>, Int>() {

            }
        ) {
            testGenericsCalls()



            with(object : BaseTestGenerics<ArrayList<ITestConcreteClass>, ArrayList<ITestConcreteClass>,
                    List<ArrayList<ITestConcreteClass>>, Int>.SomeInnerClass<String>() {

            }) {
            }
        }

        with(object : BaseTestGenerics.Extendor<ArrayList<ITestConcreteClass>() {

        }) {

        }
    }

    @Test
    fun testInnerExtender() {
        with(object : BaseTestInnerExtender(0, ITestOtherClass.create()) {

        }) {
            testInnerExtenderCalls()
        }

        with(object : BaseTestInnerExtender(0, ITestOtherClass.create()) {
            //TODO
        }) {
        }
    }


    @Test
    fun testInterface() {
        //TODO
        object : BaseTestInterface {

        }.apply {

        }
    }

    @Test
    fun testLambdaInterface() {
        //TODO
        object : BaseTestLambdaInterface {

        }.apply {

        }
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
