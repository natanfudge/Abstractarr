package test;

import org.junit.jupiter.api.Test;
import v1.net.minecraft.ITestNormalClassExtender;

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


}
