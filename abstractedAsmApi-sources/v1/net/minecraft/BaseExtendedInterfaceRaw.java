package v1.net.minecraft;

import net.minecraft.TestInterface;

public interface BaseExtendedInterfaceRaw<T extends TestInterface & ITestInterface> extends IExtendedInterfaceRaw<T> {
}
