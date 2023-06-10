package net.minecraft;

public class TestProtectedSub extends TestProtected {
    protected static final String constant = "constant";
    protected static String staticField = "static";
    protected final String instanceFinal = "instanceFinal";
    protected String instance = "instance";

    protected void fooNoMc() {
    }

    protected void fooMc(TestOtherClass mc) {
    }

    protected static void staticNoMc() {

    }

    protected static void staticMc(TestOtherClass mc) {

    }

    protected void newMethod(TestOtherClass mc){

    }
}
