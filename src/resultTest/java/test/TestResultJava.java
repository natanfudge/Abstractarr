package test;

import net.minecraft.TestAbstractImpl;
import net.minecraft.TestConcreteClass;
import org.junit.jupiter.api.Test;
import v1.net.minecraft.*;

import java.util.ArrayList;

public class TestResultJava {

    private static <T> void assertEquals(T actual, T expected) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);

        ITestConcreteClass x = new BaseTestConcreteClass() {
            @Override
            public int publicInt(ITestOtherClass iTestOtherClass) {
                return 3;
            }
        };
    }

    @Test
    void testNormalClassExtender() {
        // Not sure wtf is going on with kotlin analyzer and this class
        ITestNormalClassExtender obj = ITestNormalClassExtender.create();
        ArrayList<String> asSuper = obj.asSuper();
        asSuper.add("foo");
        assertEquals(asSuper.size(), 1);
        assertEquals(asSuper.get(0), "foo");
        asSuper.remove("foo");
        assert (asSuper.isEmpty());

        assertEquals(obj.foo(), "123");
        assertEquals(asSuper.stream(), null);
    }

    @Test
    void test(){
        TestConcreteClass expectedParam = new TestConcreteClass();
        BaseTestAbstractImpl x = new BaseTestAbstractImpl(0, ITestAbstractImpl.create(0, null)) {

            @Override
            public ITestAbstractClass abstractMethodParam(ITestConcreteClass p0)  {
                assertEquals(p0, expectedParam);
                return null;
            }
        };
        TestAbstractImpl mcThis = (TestAbstractImpl)(Object)x;
        mcThis.abstractMethodParam(expectedParam);


//            assertEquals(expectedObj1, mcThis.abstractMethod())
//            assertEquals(expectedObj2, mcThis.abstractMethodParam(expectedParam))

    }

}
