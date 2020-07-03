package v1.net.minecraft;

import java.util.ArrayList;
import net.minecraft.TestNormalClassExtenderWithMcGeneric;
import net.minecraft.TestOtherClass;
import org.jetbrains.annotations.NotNull;

public final interface ITestNormalClassExtenderWithMcGeneric {
    default ArrayList<TestOtherClass> asSuper() {
        return (ArrayList)this;
    }

    @NotNull
    static ITestNormalClassExtenderWithMcGeneric create() {
        return new TestNormalClassExtenderWithMcGeneric();
    }

    default String foo() {
        return ((TestNormalClassExtenderWithMcGeneric)this).foo();
    }

    @NotNull
    static ITestNormalClassExtenderWithMcGeneric[] array(int var0) {
        return new TestNormalClassExtenderWithMcGeneric[var0];
    }
}
