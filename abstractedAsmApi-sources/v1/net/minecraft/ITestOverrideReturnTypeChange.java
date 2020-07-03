package v1.net.minecraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.TestOverrideReturnTypeChange;
import org.jetbrains.annotations.NotNull;

public final interface ITestOverrideReturnTypeChange extends ITestOverrideReturnTypeChangeSuper {
    @NotNull
    static ITestOverrideReturnTypeChange create() {
        return new TestOverrideReturnTypeChange();
    }

    default List foo() {
        return ((TestOverrideReturnTypeChange)this).foo();
    }

    default ArrayList<ITestOtherClass> bar() {
        return ((TestOverrideReturnTypeChange)this).bar();
    }

    default ITestAbstractImpl mcClass() {
        return ((TestOverrideReturnTypeChange)this).mcClass();
    }

    @NotNull
    static ITestOverrideReturnTypeChange[] array(int var0) {
        return new TestOverrideReturnTypeChange[var0];
    }
}
