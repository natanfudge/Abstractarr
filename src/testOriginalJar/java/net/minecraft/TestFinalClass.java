package net.minecraft;


public final class TestFinalClass extends TestSuperClass  {
    public TestOtherClass publicField;
    public final int publicFinalField = 2;
    public static String publicStaticField;
    public static final TestOtherClass publicStaticOtherClassField = new TestOtherClass();

    public TestFinalClass(TestOtherClass otherParam) {
        super(otherParam);
    }

    public int publicInt(TestOtherClass someClass) {
        return 2;
    }
    public final int publicFinalInt() {
        return 2;
    }

}
