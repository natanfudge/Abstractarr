package test

import net.minecraft.TestThrowable
import org.junit.jupiter.api.Test
import v1.net.minecraft.*
import java.io.IOException

fun ITestAbstractImpl.testAbstractImplCalls() {
    assert(abstractMethod() is ITestAbstractClass)
    assert(abstractMethodParam(ITestConcreteClass.create(0, null)) is ITestAbstractClass)
    assertEquals(field, 0)
    field = 2
    assertEquals(field, 2)
    assertEquals(foo(), null)
    assertEquals(bar(), 2)
    assertEquals(compareTo(ITestAbstractImpl.create(0, null)), 0)
}

fun ITestConcreteClass.testConcreteClassCalls() {
    assertEquals(publicInt(null), 2)
    assertEquals(mutatesField(), 123)
    assertEquals(finalMethod(), 3)

    val value = innerClassMethod()
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

fun ITestClashingNames.testClashingNamesCalls() {
    assertEquals(isSomeBool_field, false)
    isSomeBool = true
    assertEquals(isSomeBool_field, true)
    assertEquals(isSomeBool, false)
    assertEquals(someInt, 0)
    someInt = 2
    assertEquals(someInt, 2)
    assertEquals(getSomeInt(3), 0)
}

fun ITestInnerExtender.testInnerExtenderCalls() {
    assertEquals(publicInt(), 2)
    assertEquals(inheritedMethod(), 2)
    normalMethod()
}

fun ITestAnnotations.testAnnotationsCalls() {
    val x: ITestAbstractClass? = abstractMethod()
    val y = foo(2, "foo")
    //            val y = foo(2, null)
    val z = nullable()
    //            z.length
    val a = instanceField
    //            a.length
}

fun ITestArrays.testArraysCalls() {
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

fun ITestNormalClassExtender.testNormalClassExtenderCalls() {
    val asSuper = asSuper()
    asSuper.add("foo")
    assertEquals(asSuper.size, 1)
    assertEquals(asSuper[0], "foo")
    asSuper.remove("foo")
    assert(asSuper.isEmpty())

    assertEquals(foo(), "123")
    assertEquals(asSuper.stream(), null)
}


fun <T> assertEquals(actual: T, expected: T) = kotlin.test.assertEquals(expected, actual)

fun ITestConcreteClass.TestInnerClass.testConcreteInnerCalls() {
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

fun ITestGenerics<ArrayList<ITestConcreteClass>, ArrayList<ITestConcreteClass>, List<ArrayList<ITestConcreteClass>>, Int>.testGenericsCalls() {
    val x: ITestInterface = ITestAbstractImpl.create(0, null)
    val y: ArrayList<ITestConcreteClass>? = genericMethod<ArrayList<ITestConcreteClass>>(
        ArrayList(),
        ArrayList(),
        listOf(),
        listOf(),
        listOf(ITestAbstractImpl.create(0, null)),
        mutableListOf(x),
        listOf(1, 2, "3"),
        null
    )

    assertEquals(compareTo(null), 2)


    genericField1 = ArrayList()
    genericField1.add(ITestConcreteClass.create())
    assert(genericField1[0] is ITestConcreteClass)

    val f2: List<*>? = genericField2
    genericField2 = listOf<Nothing>()
    assertEquals(genericField2, listOf<Nothing>())
    val f3: List<ArrayList<ITestConcreteClass>>? = genericField3
    genericField3 = listOf()
    val f4: List<*>? = genericField4
    genericField4 = mutableListOf()
}


fun ITestOtherClass.testOtherClassCalls() {
    oneCastTest(ITestConcreteClass.create(0, null))
    twoCastTest(ITestConcreteClass.create(0, null))
    realFinalCastTest(ITestFinalClass.create(ITestOtherClass.create()))
}


fun ITestOverrideReturnTypeChange.testOverrideReturnTypeChangeCalls() {
    val x: List<*>? = foo()
    assertEquals(x, null)
    val y: ArrayList<ITestOtherClass>? = bar()
    assertEquals(y, null)
    val z: ITestAbstractImpl? = mcClass()
    assertEquals(z, null)
}


fun ITestOverrideReturnTypeChangeSuper.testOverrideReturnTypeChangeSuperCalls() {
    val x: Any? = foo()
    assertEquals(x, null)
    val y: List<ITestOtherClass>? = bar()
    assertEquals(y, null)
    val z: ITestAbstractClass? = mcClass()
    assertEquals(z, null)
}

fun ITestThrows.testThrowsCalls() {
    foo()
    bar<IOException>()
    checked()
}

fun ITestOverload.testOverloadCalls() {
    x()
    x(2)
}

@Suppress("USELESS_IS_CHECK", "UNUSED_VARIABLE")
class TestInterfaces {
    @Test
    fun testAbstractImpl() {
        println("Running testAbstractImpl test")
        with(ITestAbstractImpl.create(0, null)) {
            testAbstractImplCalls()
        }
    }

    @Test
    fun testAnnotations() {
        with(ITestAnnotations.create()) {
            testAnnotationsCalls()
        }
    }

    @Test
    fun testArrays() {
        with(ITestArrays.create()) {
            testArraysCalls()
        }
    }


    @Test
    fun testClashingNames() {
        with(ITestClashingNames.create()) {
            testClashingNamesCalls()
        }
    }


    @Test
    fun testConcreteClass() {

        ITestConcreteClass.publicStaticOtherClassField
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
            testConcreteClassCalls()
        }

        with(ITestConcreteClass.create(0, ITestOtherClass.create()).newTestInnerClass(123, ITestOtherClass.create())) {
            testConcreteInnerCalls()
        }

        with(ITestConcreteClass.TestStaticInnerClass.create(1, ITestOtherClass.create())) {
            assertEquals(publicInt(), 2)
        }

    }

    @Test
    fun testEnum() {
        assertEquals(ITestEnum.foo(), ITestEnum.THING2)
        with(ITestEnum.THING) {
            assertEquals(bar(), null)
            assertEquals(x, 1)
            x = 3
            assertEquals(x, 3)
        }

        with(ITestEnum.THING2 as Enum<*>) {
            assertEquals(name, "THING2")
            assertEquals(ordinal, 1)
        }

        with(ITestEnum.values()) {
            assertEquals(size, 2)
            assertEquals(this[0], ITestEnum.THING)
            assertEquals(this[1], ITestEnum.THING2)
        }

        assertEquals(ITestEnum.valueOf("THING"), ITestEnum.THING)
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
    fun testGenerics() {
        with(
            ITestGenerics.create<ArrayList<ITestConcreteClass>, ArrayList<ITestConcreteClass>,
                    List<ArrayList<ITestConcreteClass>>, Int>()
        ) {
            testGenericsCalls()



            with(newSomeInnerClass<Int>()) {
                val i1: Int? = useInnerClassGeneric()
                val i2: ArrayList<ITestConcreteClass>? = useOuterClassGeneric()
            }
        }

        ITestGenerics.Extendor.create<ArrayList<ITestConcreteClass>>()




        ITestGenerics.SomeInnerClass.array<ArrayList<ITestConcreteClass>, ArrayList<ITestConcreteClass>,
                List<ArrayList<ITestConcreteClass>>, Int, Int>(5)
    }

    @Test
    fun testInnerExtender() {
        with(ITestInnerExtender.create(0, ITestOtherClass.create())) {
            testInnerExtenderCalls()
        }
    }

    @Test
    fun testInterface() {
        assertEquals(ITestInterface.x, 2)
    }

    @Test
    fun testLambdaInterface() {
//        val x : ITestLambdaInterface = ITestLambdaInterface {null}
    }

    @Test
    fun testLambdaAnons() {
        ITestLambdasAnons.foo()
    }

    @Test
    fun testNormalClassExtender() {
        ITestNormalClassExtender.create().apply {
            testNormalClassExtenderCalls()
        }
    }

    @Test
    fun testOverload() {
        with(ITestOverload.create()) {
            testOverloadCalls()
        }
    }


    @Test
    fun testOtherClass() {
        with(ITestOtherClass.create()) {
            testOtherClassCalls()
        }
    }

    @Test
    fun testOverrideReturnTypeChange() {
        with(ITestOverrideReturnTypeChange.create()) {
            testOverrideReturnTypeChangeCalls()
        }
    }


    @Test
    fun testOverrideReturnTypeChangeSuper() {
        with(ITestOverrideReturnTypeChangeSuper.create()) {
            testOverrideReturnTypeChangeSuperCalls()
        }
    }

    @Test
    fun testSuperClass() {
        assertEquals(ITestSuperClass.getStaticField(), "static")
        ITestSuperClass.setStaticField("foo")
        assertEquals(ITestSuperClass.getStaticField(), "foo")
        with(ITestSuperClass.create(ITestOtherClass.create())) {
            assertEquals(inheritedMethod(), 2)
        }
    }

    @Test
    fun testThrowable() {
        var catched = false
        try {
            throw TestThrowable("foo")
        } catch (e: Throwable) {
            if (e is ITestThrowable) {
                catched = true
            }
        }
        assertEquals(catched, true)
    }

//    fun tryCatch(e)

    @Test
    fun testThrows() {
        try {
            with(ITestThrows.create()) {
                testThrowsCalls()
            }
        } catch (e: Exception) {

        }

    }


}
