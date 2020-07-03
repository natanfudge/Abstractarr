package v1.net.minecraft;

import net.minecraft.TestOverload;
import org.jetbrains.annotations.NotNull;

public final interface ITestOverload {
    @NotNull
    static ITestOverload create() {
        return new TestOverload();
    }

    default void x() {
        ((TestOverload)this).x();
    }

    default void x(int var1) {
        ((TestOverload)this).x(var1);
    }

    @NotNull
    static ITestOverload[] array(int var0) {
        return new TestOverload[var0];
    }
}
