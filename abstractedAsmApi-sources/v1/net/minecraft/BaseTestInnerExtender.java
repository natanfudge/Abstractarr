package v1.net.minecraft;

import net.minecraft.TestInnerExtender;
import net.minecraft.TestOtherClass;

public abstract class BaseTestInnerExtender implements ITestInnerExtender {
    protected static String protectedStatic() {
        return TestInnerExtender.protectedStatic();
    }

    public BaseTestInnerExtender(int var1, ITestOtherClass var2) {
        super(var1, (TestOtherClass)var2);
    }
}
