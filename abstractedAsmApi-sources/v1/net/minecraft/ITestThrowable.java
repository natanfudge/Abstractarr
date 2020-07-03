package v1.net.minecraft;

import net.minecraft.TestThrowable;
import org.jetbrains.annotations.NotNull;

public final interface ITestThrowable {
    default Throwable asSuper() {
        return (Throwable)this;
    }

    @NotNull
    static ITestThrowable create(String var0) {
        return new TestThrowable(var0);
    }

    @NotNull
    static ITestThrowable[] array(int var0) {
        return new TestThrowable[var0];
    }
}
