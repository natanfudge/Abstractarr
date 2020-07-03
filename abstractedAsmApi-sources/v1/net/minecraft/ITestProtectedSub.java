package v1.net.minecraft;

import net.minecraft.TestProtectedSub;
import org.jetbrains.annotations.NotNull;

public final interface ITestProtectedSub extends ITestProtected {
    @NotNull
    static ITestProtectedSub create() {
        return new TestProtectedSub();
    }

    @NotNull
    static ITestProtectedSub[] array(int var0) {
        return new TestProtectedSub[var0];
    }
}
