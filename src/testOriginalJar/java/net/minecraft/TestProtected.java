package net.minecraft;

public class TestProtected {
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


    protected final void fooNoMcFinal() {
    }

    protected final void fooMcFinal(TestOtherClass mc) {
    }

    protected static class StaticInner {
        protected void bar(){

        }
        public void foo() {
        }
        void packagePrivateThing(){}
    }

    protected class NonStaticInner {
        private void bar(TestOtherClass mc){
            
        }
        public void baz(TestOtherClass foo){

        }
        void foo() {
            String x = instance;
        }
    }
}
