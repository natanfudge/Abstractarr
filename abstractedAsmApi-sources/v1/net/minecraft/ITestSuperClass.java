package v1.net.minecraft;

import net.minecraft.TestOtherClass;
import net.minecraft.TestSuperClass;
import org.jetbrains.annotations.NotNull;

public final interface ITestSuperClass {
    @NotNull
    static ITestSuperClass create(ITestOtherClass var0) {
        return new TestSuperClass((TestOtherClass)var0);
    }

    default int inheritedMethod() {
        return ((TestSuperClass)this).inheritedMethod();
    }

    default int overridenMethod() {
        return ((TestSuperClass)this).overridenMethod();
    }

    final default String getInheritedField() {
        return ((TestSuperClass)this).inheritedField;
    }

    final default void setInheritedField(String var1) {
        ((TestSuperClass)this).inheritedField = var1;
    }

    static final String getStaticField() {
        return TestSuperClass.staticField;
    }

    static final void setStaticField(String var0) {
        TestSuperClass.staticField = var0;
    }

    @NotNull
    static ITestSuperClass[] array(int var0) {
        return new TestSuperClass[var0];
    }
}
