package v1.net.minecraft;

import net.minecraft.TestArrays;
import net.minecraft.TestConcreteClass;
import org.jetbrains.annotations.NotNull;

public final interface ITestArrays {
    @NotNull
    static ITestArrays create() {
        return new TestArrays();
    }

    default ITestConcreteClass[] arrMethod() {
        return ((TestArrays)this).arrMethod();
    }

    default void arrParam(ITestConcreteClass[] var1) {
        ((TestArrays)this).arrParam((TestConcreteClass[])var1);
    }

    final default ITestConcreteClass[] getArrField() {
        return ((TestArrays)this).arrField;
    }

    final default void setArrField(ITestConcreteClass[] var1) {
        ((TestArrays)this).arrField = (TestConcreteClass[])var1;
    }

    @NotNull
    static ITestArrays[] array(int var0) {
        return new TestArrays[var0];
    }
}
