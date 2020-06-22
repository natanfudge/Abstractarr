package test;

import net.minecraft.TestAbstractImpl;
import net.minecraft.TestConcreteClass;
import org.junit.jupiter.api.Test;
import v1.net.minecraft.*;

import java.io.IOException;
import java.util.ArrayList;

public class TestResultJava {

    private static <T> void assertEquals(T actual, T expected) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
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
    void testOverrideExample() {
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
    }

    @Test
    void testThrows(){
        try {
            ITestThrows x = ITestThrows.create();
            x.checked();
        } catch (IOException | RuntimeException e) {
//            e.printStackTrace();
        }
    }

}
