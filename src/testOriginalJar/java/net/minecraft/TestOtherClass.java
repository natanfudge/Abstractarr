package net.minecraft;


import v1.net.minecraft.ITestOtherClass;

public class TestOtherClass implements ITestOtherClass {
    public void oneCastTest(TestConcreteClass x) {
        ((TestInterface)x).bar();
    }

    public void twoCastTest(TestConcreteClass x) {
        ((TestInterface)(Object)x).bar();
    }

    public void realFinalCastTest(TestFinalClass x) {
        ((TestInterface)(Object)x).bar();
    }

//    public void arrayCastSubSuperTest(TestFinalClass[] x) {
//        TestInterface[] arr = ((TestInterface[])(Object)x);
//    }
//
//
//    public void arrayCastSuperSubTest(TestInterface[] x) {
//        TestFinalClass[] arr = ((TestFinalClass[])(Object)x);
//    }
}
