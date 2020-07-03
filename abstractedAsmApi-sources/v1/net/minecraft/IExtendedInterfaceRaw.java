package v1.net.minecraft;

import net.minecraft.ExtendedInterfaceRaw;
import org.jetbrains.annotations.NotNull;

public final interface IExtendedInterfaceRaw<T extends ITestInterface> {
    @NotNull
    static <T extends ITestInterface> IExtendedInterfaceRaw<T>[] array(int var0) {
        return new ExtendedInterfaceRaw[var0];
    }
}
