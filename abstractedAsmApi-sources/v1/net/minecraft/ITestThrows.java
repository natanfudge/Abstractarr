package v1.net.minecraft;

import java.io.IOException;
import net.minecraft.TestThrows;
import org.jetbrains.annotations.NotNull;

public final interface ITestThrows {
    @NotNull
    static ITestThrows create() throws RuntimeException {
        return new TestThrows();
    }

    default void foo() throws NullPointerException {
        ((TestThrows)this).foo();
    }

    default void checked() throws IOException {
        ((TestThrows)this).checked();
    }

    default <T extends Throwable> void bar() throws T {
        ((TestThrows)this).bar();
    }

    @NotNull
    static ITestThrows[] array(int var0) {
        return new TestThrows[var0];
    }
}
