package v1.net.minecraft;

import net.minecraft.TestConcreteClass;
import net.minecraft.TestOtherClass;

public abstract class BaseTestConcreteClass implements ITestConcreteClass {
    protected static String protectedStatic() {
        return TestConcreteClass.protectedStatic();
    }

    protected final int getProtectedField() {
        return super.protectedField;
    }

    protected final void setProtectedField(int var1) {
        super.protectedField = var1;
    }

    public BaseTestConcreteClass(int var1, ITestOtherClass var2) {
        super(var1, (TestOtherClass)var2);
    }

    public BaseTestConcreteClass() {
    }

    public abstract static class TestStaticInnerClass implements ITestConcreteClass.TestStaticInnerClass {
        protected static String protectedStatic() {
            return TestConcreteClass.TestStaticInnerClass.protectedStatic();
        }

        public TestStaticInnerClass(int var1, ITestOtherClass var2) {
            super(var1, (TestOtherClass)var2);
        }
    }
}
