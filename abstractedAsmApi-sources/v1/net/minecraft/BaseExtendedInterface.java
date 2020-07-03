package v1.net.minecraft;

import net.minecraft.TestInterface;

public interface BaseExtendedInterface<T extends TestInterface & ITestInterface> extends IExtendedInterface<T> {
}
