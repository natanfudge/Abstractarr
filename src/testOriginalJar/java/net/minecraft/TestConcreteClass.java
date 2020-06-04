package net.minecraft;

import v1.net.minecraft.ITestConcreteClass;

public class TestConcreteClass  extends TestSuperClass implements ITestConcreteClass {
    private int privateField;
    int packageField;
    protected int protectedField;
    public int publicField;
    public final int publicFinalField = 2;
    public TestOtherClass otherClassField = new TestOtherClass();
    private static String privateStaticField;
    public static String publicStaticField;
    public static final TestOtherClass publicStaticOtherClassField = new TestOtherClass();
    public static final String publicStaticFinalField = "BAR";

    private void privateVoid() {
        privateField = 7;
    }

    public int publicInt() {
        return 2;
    }

    protected static String protectedStatic() {
        return "SomeString";
    }

    public static int publicStatic() {
        return 4;
    }

    int packageArgs(TestOtherClass arg1, int arg2) {
        return 1;
    }

    public int mutatesField() {
        privateField++;
        return 123;
    }

    public final int finalMethod() {
        return 3;
    }

    public TestStaticInnerClass innerClassMethod(){return new TestStaticInnerClass(23, new TestOtherClass());}

    public TestConcreteClass(int arg1, TestOtherClass arg2) {
        super(arg2);
    }

    public static class TestStaticInnerClass implements ITestConcreteClass.TestStaticInnerClass{
        public static String publicStaticField;
        public static final TestOtherClass publicStaticOtherClassField = new TestOtherClass();

        protected static String protectedStatic() {
            return "SomeString";
        }

        public static int publicStatic() { return 4;
        }

        public TestStaticInnerClass(int arg1, TestOtherClass arg2) {

        }

        public int publicField;
        public final int publicFinalField = 2;
        public TestOtherClass otherClassField;
        public static final String publicStaticFinalField = "BAR";

        public int publicInt() {
            return 2;
        }

        public int mutatesField() {
            publicField++;
            return 123;
        }

        public final int finalMethod() {
            return 3;
        }

    }

    public class TestInnerClass implements ITestConcreteClass.TestInnerClass {
        public TestInnerClass(int arg1, TestOtherClass arg2) {

        }

        public int publicField;
        public final int publicFinalField = 2;
        public TestOtherClass otherClassField = new TestOtherClass();
        public static final String publicStaticFinalField = "BAR";

        public int publicInt() {
            return 2;
        }

        public int mutatesField() {
            publicField++;
            return 123;
        }

        public final int finalMethod() {
            return 3;
        }

    }

}
