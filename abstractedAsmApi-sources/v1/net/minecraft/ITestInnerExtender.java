package v1.net.minecraft;

import net.minecraft.TestInnerExtender;
import net.minecraft.TestOtherClass;
import org.jetbrains.annotations.NotNull;

public final interface ITestInnerExtender extends ITestConcreteClass.TestStaticInnerClass {
    @NotNull
    static ITestInnerExtender create(int var0, ITestOtherClass var1) {
        return new TestInnerExtender(var0, (TestOtherClass)var1);
    }

    default void normalMethod() {
        ((TestInnerExtender)this).normalMethod();
    }

    @NotNull
    static ITestInnerExtender[] array(int var0) {
        return new TestInnerExtender[var0];
    }
}
