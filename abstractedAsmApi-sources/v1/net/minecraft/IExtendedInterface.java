package v1.net.minecraft;

import net.minecraft.ExtendedInterface;
import org.jetbrains.annotations.NotNull;

public final interface IExtendedInterface<T extends ITestInterface> {
    @NotNull
    static <T extends ITestInterface> IExtendedInterface<T>[] array(int var0) {
        return new ExtendedInterface[var0];
    }
}
