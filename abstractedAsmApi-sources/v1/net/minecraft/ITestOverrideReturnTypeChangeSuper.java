package v1.net.minecraft;

import java.util.List;
import net.minecraft.TestOverrideReturnTypeChangeSuper;
import org.jetbrains.annotations.NotNull;

public final interface ITestOverrideReturnTypeChangeSuper {
    @NotNull
    static ITestOverrideReturnTypeChangeSuper create() {
        return new TestOverrideReturnTypeChangeSuper();
    }

    default Object foo() {
        return ((TestOverrideReturnTypeChangeSuper)this).foo();
    }

    default List<ITestOtherClass> bar() {
        return ((TestOverrideReturnTypeChangeSuper)this).bar();
    }

    default ITestAbstractClass mcClass() {
        return ((TestOverrideReturnTypeChangeSuper)this).mcClass();
    }

    @NotNull
    static ITestOverrideReturnTypeChangeSuper[] array(int var0) {
        return new TestOverrideReturnTypeChangeSuper[var0];
    }
}
