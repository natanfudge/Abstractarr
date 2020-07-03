package v1.net.minecraft;

import java.util.ArrayList;
import net.minecraft.TestNormalClassExtender;
import org.jetbrains.annotations.NotNull;

public final interface ITestNormalClassExtender {
    default ArrayList<String> asSuper() {
        return (ArrayList)this;
    }

    @NotNull
    static ITestNormalClassExtender create() {
        return new TestNormalClassExtender();
    }

    default String foo() {
        return ((TestNormalClassExtender)this).foo();
    }

    @NotNull
    static ITestNormalClassExtender[] array(int var0) {
        return new TestNormalClassExtender[var0];
    }
}
