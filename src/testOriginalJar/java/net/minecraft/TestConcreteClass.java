package net.minecraft;

public class TestConcreteClass extends TestSuperClass {
    private int privateField;
    int packageField;
    protected int protectedField;
    public int publicField;
    public final int publicFinalField = 2;
    public TestOtherClass otherClassField;
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

    public static int publicStatic(){
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

//    public TestInnerClass innerClassMethod(){return null;}

    public TestConcreteClass(int arg1, TestOtherClass arg2) {
        super(arg2);
    }

//    public class TestInnerClass {
//        public int publicField;
//        public final int publicFinalField = 2;
//        public TestOtherClass otherClassField;
//        public static final String publicStaticFinalField = "BAR";
//
//        public int publicInt() {
//            return 2;
//        }
//
//        public int mutatesField() {
//            privateField++;
//            return 123;
//        }
//
//        public final int finalMethod() {
//            return 3;
//        }
//
//        public TestInnerClass(int arg1, TestOtherClass arg2) {
//
//        }
//    }

    public static class TestStaticInnerClass {
        public static String publicStaticField;
        public static final TestOtherClass publicStaticOtherClassField = new TestOtherClass();

        protected static String protectedStatic() {
            return "SomeString";
        }

        public static int publicStatic(){
            return 4;
        }

        public TestStaticInnerClass(int arg1, TestOtherClass arg2) {

        }

    }

}
