package v1.net.minecraft;

import net.minecraft.TestFinalClass;
import net.minecraft.TestOtherClass;
import org.jetbrains.annotations.NotNull;

public final interface ITestFinalClass extends ITestSuperClass {
    ITestOtherClass publicStaticOtherClassField = TestFinalClass.publicStaticOtherClassField;

    @NotNull
    static ITestFinalClass create(ITestOtherClass var0) {
        return new TestFinalClass((TestOtherClass)var0);
    }

    default int publicInt(ITestOtherClass var1) {
        return ((TestFinalClass)this).publicInt((TestOtherClass)var1);
    }

    final default int publicFinalInt() {
        return ((TestFinalClass)this).publicFinalInt();
    }

    final default ITestOtherClass getPublicField() {
        return ((TestFinalClass)this).publicField;
    }

    final default void setPublicField(ITestOtherClass var1) {
        ((TestFinalClass)this).publicField = (TestOtherClass)var1;
    }

    final default int getPublicFinalField() {
        return ((TestFinalClass)this).publicFinalField;
    }

    static final String getPublicStaticField() {
        return TestFinalClass.publicStaticField;
    }

    static final void setPublicStaticField(String var0) {
        TestFinalClass.publicStaticField = var0;
    }

    @NotNull
    static ITestFinalClass[] array(int var0) {
        return new TestFinalClass[var0];
    }
}
