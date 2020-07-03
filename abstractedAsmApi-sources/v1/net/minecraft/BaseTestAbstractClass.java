package v1.net.minecraft;

import net.minecraft.TestAbstractClass;

public abstract class BaseTestAbstractClass implements ITestAbstractClass, ITestInterface {
    public BaseTestAbstractClass(int var1, ITestAbstractClass var2) {
        super(var1, (TestAbstractClass)var2);
    }
}
