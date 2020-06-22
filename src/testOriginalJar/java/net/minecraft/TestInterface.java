package net.minecraft;

public interface TestInterface {
    TestInterface foo();

    default int bar() {
        return 2;
    }

    default TestOtherClass baz() {
        return null;
    }

    default TestOtherClass baz(TestOtherClass x) {
        return null;
    }

    TestOtherClass boz(TestOtherClass x);

    int x = 2;

    public static void testStatic(){

    }

    public static void testStaticParam(TestOtherClass mc) {

    }
}
