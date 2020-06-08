package net.minecraft;

public class TestInnerExtender extends TestConcreteClass.TestStaticInnerClass {
    public TestInnerExtender(int arg1, TestOtherClass arg2) {
        super(arg1, arg2);
    }

    @Override
    public int inheritedMethod() {
        return super.inheritedMethod();
    }

    public void normalMethod(){}
}
