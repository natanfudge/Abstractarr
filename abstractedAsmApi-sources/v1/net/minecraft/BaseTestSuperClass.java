package v1.net.minecraft;

import net.minecraft.TestOtherClass;

public abstract class BaseTestSuperClass implements ITestSuperClass {
    public BaseTestSuperClass(ITestOtherClass var1) {
        super((TestOtherClass)var1);
    }
}
