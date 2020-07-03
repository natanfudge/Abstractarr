package v1.net.minecraft;

import net.minecraft.TestConcreteClass;
import net.minecraft.TestFinalClass;
import net.minecraft.TestOtherClass;
import org.jetbrains.annotations.NotNull;

public final interface ITestOtherClass {
    @NotNull
    static ITestOtherClass create() {
        return new TestOtherClass();
    }

    default void oneCastTest(ITestConcreteClass var1) {
        ((TestOtherClass)this).oneCastTest((TestConcreteClass)var1);
    }

    default void twoCastTest(ITestConcreteClass var1) {
        ((TestOtherClass)this).twoCastTest((TestConcreteClass)var1);
    }

    default void realFinalCastTest(ITestFinalClass var1) {
        ((TestOtherClass)this).realFinalCastTest((TestFinalClass)var1);
    }

    @NotNull
    static ITestOtherClass[] array(int var0) {
        return new TestOtherClass[var0];
    }
}
