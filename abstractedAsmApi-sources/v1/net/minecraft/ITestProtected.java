package v1.net.minecraft;

import net.minecraft.TestProtected;
import org.jetbrains.annotations.NotNull;

public final interface ITestProtected {
    @NotNull
    static ITestProtected create() {
        return new TestProtected();
    }

    @NotNull
    static ITestProtected[] array(int var0) {
        return new TestProtected[var0];
    }

    public final interface NonStaticInner {
    }

    public final interface StaticInner {
    }
}
