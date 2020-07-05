package test

import net.minecraft.*
import org.junit.jupiter.api.Test
import testBaseclass
import testBaseclassExtendor
import testMcOnly
import v1.net.minecraft.*
import java.util.stream.Stream


@Suppress("CAST_NEVER_SUCCEEDS", "UNCHECKED_CAST")
class TestBaseClasses {
    private fun <T> assertEquals(actual: T, expected: T) = kotlin.test.assertEquals(expected, actual)


    @Test
    fun testAbstractClass() {
        val e1 = ITestAbstractImpl.create(0, null)
        val e2 = ITestAbstractImpl.create(1, null)
        val e3 = ITestOtherClass.create()
        val e4 = ITestAnnotations.create()
        val e5 = ITestOtherClass.create()
        object : BaseTestAbstractClass(0, null) {
            override fun abstractMethod(): ITestAbstractClass {
                return e1
            }

            override fun abstractMethodParam(p0: ITestConcreteClass?): ITestAbstractClass {
                return e2
            }

            override fun boz(p0: ITestOtherClass?): ITestOtherClass {
                return e3
            }

            override fun foo(): ITestInterface {
                return e4
            }

            override fun baz(): ITestOtherClass {
                return e5
            }
        }.apply {
            this as TestAbstractClass
            assertEquals(abstractMethod(), e1)
            assertEquals(abstractMethodParam(null), e2)
            assertEquals(boz(null), e3)
            assertEquals(foo(), e4)
            assertEquals(baz(), e5)
        }
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
            init {
                assertEquals(protectedStatic(),"SomeString")
            }
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
            assertEquals(inheritedField, "inherited")
            assertEquals(finalMethod(), 3)
            this as TestInnerExtender
            assertEquals(inheritedMethod(), 5)
            assertEquals(overridenMethod(), 6)

            assertEquals(ITestConcreteClass.TestStaticInnerClass.publicStatic(), 4)
            assertEquals(publicInt(), 7)
            assertEquals(mutatesField(), 8)
            assertEquals(finalMethod(), 3)
            normalMethod()
            assertEquals(x, 9)

        }
    }

    @Test
    fun testInterface() {
        val e1 = ITestAbstractImpl.create(0, null)
        val e2 = ITestOtherClass.create()
        val e3 = ITestOtherClass.create()
        object : BaseTestInterface {
            override fun foo(): ITestInterface {
                return e1
            }

            override fun boz(p0: ITestOtherClass?): ITestOtherClass {
                return e2
            }

            override fun baz(): ITestOtherClass {
                return e3
            }
        }.apply {
            this as TestInterface
            assertEquals(foo(), e1)
            assertEquals(boz(null), e2)
            assertEquals(baz(), e3)
        }
    }

    @Test
    fun testLambdaInterface() {
        val e = BaseTestLambdaInterface { null }
        BaseTestLambdaInterface { e }.apply {
            this as TestLambdaInterface
            assertEquals(foo(), e)
        }
        object : BaseTestLambdaInterface {
            override fun foo(): ITestLambdaInterface {
                return e
            }
        }.apply {
            this as TestLambdaInterface
            assertEquals(foo(), e)
        }
    }


    @Test
    fun testNormalClassExtender() {
        object : BaseTestNormalClassExtender() {

        }.apply {
            testNormalClassExtenderCalls()
        }

        val e = listOf(ITestOtherClass.create()).stream()

        object : BaseTestNormalClassExtenderWithMcGeneric() {
            override fun foo(): String {
                return "override"
            }

            override fun isEmpty(): Boolean {
                return false
            }

            override fun stream(): Stream<ITestOtherClass> {
                return  e
            }
        }.apply {
            this as TestNormalClassExtenderWithMcGeneric
            testMcOnly(e as Stream<TestOtherClass>)
            assertEquals(foo(), "override")
        }
    }

    @Test
    fun testOverload() {

        object : BaseTestOverload() {

        }.apply {
            testOverloadCalls()
        }

        var called = false
        object : BaseTestOverload() {
            override fun x() {
                called = true
            }

            override fun x(p0: Int) {
                assertEquals(p0,10)
            }
        }.apply {
            this as TestOverload
            x()
            x(10)
            assertEquals(called,true)
        }
    }

    @Test
    fun testOtherClass() {
        object : BaseTestOtherClass() {

        }.apply {
            testOtherClassCalls()
        }

        val e1 = ITestConcreteClass.create()
        val e2 = ITestConcreteClass.create()
        val e3 = ITestFinalClass.create(null)

        object : BaseTestOtherClass() {
            override fun oneCastTest(p0: ITestConcreteClass?) {
                assertEquals(p0,e1)
            }

            override fun twoCastTest(p0: ITestConcreteClass?) {
                assertEquals(p0,e2)
            }

            override fun realFinalCastTest(p0: ITestFinalClass?) {
                assertEquals(p0,e3)
            }
        }.apply {
            this as TestOtherClass
            oneCastTest(e1)
            twoCastTest(e2)
            realFinalCastTest(e3)
        }

    }

    @Test
    fun testOverrideReturnTypeChange() {
        object : BaseTestOverrideReturnTypeChange() {

        }.apply {
            testOverrideReturnTypeChangeCalls()
        }

        val e1 = mutableListOf<Any?>()
        val e2 = arrayListOf<ITestOtherClass>()
        val e3 = ITestAbstractImpl.create(0,null)
        object : BaseTestOverrideReturnTypeChange() {
            override fun foo(): MutableList<Any?> {
                return e1
            }

            override fun bar(): java.util.ArrayList<ITestOtherClass> {
                return e2
            }

            override fun mcClass(): ITestAbstractImpl {
                return e3
            }
        }.apply {
            val x = this as TestOverrideReturnTypeChange
            assertEquals(foo(), e1)
            assertEquals(x.bar(), e2)
            assertEquals(mcClass(), e3)
        }

    }

    @Test
    fun testOverrideReturnTypeChangeSuper() {
        object : TestOverrideReturnTypeChangeSuper() {

        }.apply {
            testOverrideReturnTypeChangeSuperCalls()
        }

        val e1 = mutableListOf<Any?>()
        val e2 = arrayListOf<ITestOtherClass>()
        val e3 = ITestAbstractImpl.create(0,null)
        object : BaseTestOverrideReturnTypeChangeSuper() {
            override fun foo(): Any {
                return e1
            }

            override fun bar(): MutableList<ITestOtherClass> {
                return e2
            }

            override fun mcClass(): ITestAbstractClass {
                return e3
            }
        }.apply {
            val x = this as TestOverrideReturnTypeChangeSuper
            assertEquals(foo(), e1)
            assertEquals(x.bar(), e2)
            assertEquals(mcClass(), e3)
        }
    }

    @Test
    fun testProtected() {
        var c1 = false
        var c2 = false

        object : BaseTestProtected() {
            init {
                assertEquals(constant,"constant")
                assertEquals(getStaticField(),"static")
                setStaticField("bar")
                assertEquals(getStaticField(),"bar")
                assertEquals(instanceFinal,"instanceFinal")
                assertEquals(instance,"instance")
                instance = "foo"
                assertEquals(instance,"foo")

                fooNoMc()
                assertEquals(c1,true)
                fooMc(ITestOtherClass.create())
                assertEquals(c2,true)

                staticNoMc()
                staticMc(ITestOtherClass.create())
                fooNoMcFinal()
                fooMcFinal(ITestOtherClass.create())

            }

            override fun fooNoMc() {
                c1 = true
            }

            override fun fooMc(p0: ITestOtherClass?) {
                c2 = true
            }
        }

    }

    @Test
    fun testProtectedSub() {
        var c1 = false
        var c2 = false
        var c3 = false

        object : BaseTestProtectedSub(){
            init {
                assertEquals(constant,"constant")
                assertEquals(getStaticField(),"static")
                setStaticField("bar")
                assertEquals(getStaticField(),"bar")
                assertEquals(instanceFinal,"instanceFinal")
                assertEquals(instance,"instance")
                instance = "foo"
                assertEquals(instance,"foo")

                fooNoMc()
                assertEquals(c1,true)
                fooMc(ITestOtherClass.create())
                assertEquals(c2,true)
                newMethod(ITestOtherClass.create())
                assertEquals(c3, true)

                staticNoMc()
                staticMc(ITestOtherClass.create())
                fooNoMcFinal()
                fooMcFinal(ITestOtherClass.create())
            }

            override fun fooNoMc() {
                c1 = true
            }

            override fun fooMc(p0: ITestOtherClass?) {
                c2 = true
            }

            override fun newMethod(p0: ITestOtherClass?) {
                c3 = true
            }
        }.apply {

        }

    }

    @Test
    fun testSuperClass() {
        object : BaseTestSuperClass(null) {
            override fun inheritedMethod(): Int {
                return 40
            }

            override fun overridenMethod(): Int {
                return 50
            }
        }.apply {
            this as TestSuperClass
            assertEquals(inheritedField,"inherited")
            assertEquals(inheritedMethod(),40)
            assertEquals(overridenMethod(), 50)
        }
    }


    @Test
    fun testThrows() {
        try {
            object : BaseTestThrows() {

            }.apply {
                testThrowsCalls()
            }

            var called1 = false
            var called2 = false
            var called3 = false

            object : BaseTestThrows() {
                override fun foo() {
                    called1 = true
                }

                override fun checked() {
                    called2 = true
                }

                override fun <T : Throwable?> bar() {
                    called3 = true
                }
            }.apply {
                this as TestThrows
                foo()
                checked()
                bar<Throwable>()
                assertEquals(called1,true)
                assertEquals(called2,true)
                assertEquals(called3,true)
            }

        } catch (e: Exception) {

        }

    }

}
