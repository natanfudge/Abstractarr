package net.minecraft;


public class TestOtherClass {
    public void oneCastTest(TestConcreteClass x) {
//        ((TestInterface) x).bar();
    }

    public void twoCastTest(TestConcreteClass x) {
//        ((TestInterface) (Object) x).bar();
    }

    public void realFinalCastTest(TestFinalClass x) {
//        ((TestInterface) (Object) x).bar();
    }
}
