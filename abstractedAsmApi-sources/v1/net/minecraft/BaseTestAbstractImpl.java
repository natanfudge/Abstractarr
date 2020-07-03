package v1.net.minecraft;

import net.minecraft.TestAbstractClass;

public abstract class BaseTestAbstractImpl implements ITestAbstractImpl, ITestInterface, Comparable<ITestAbstractImpl> {
    public BaseTestAbstractImpl(int var1, ITestAbstractClass var2) {
        super(var1, (TestAbstractClass)var2);
    }
}
