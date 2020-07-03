package v1.net.minecraft;

import net.minecraft.TestAnnotations;
import net.minecraft.TestConcreteClass;
import net.minecraft.TestOtherClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final interface ITestAnnotations extends ITestAbstractClass {
    @Nullable
    String finalStaticField = TestAnnotations.finalStaticField;

    @NotNull
    static ITestAnnotations create() {
        return new TestAnnotations();
    }

    @Nullable
    default ITestAbstractClass abstractMethod() {
        return ((TestAnnotations)this).abstractMethod();
    }

    @NotNull
    default ITestAbstractClass abstractMethodParam(ITestConcreteClass var1) {
        return ((TestAnnotations)this).abstractMethodParam((TestConcreteClass)var1);
    }

    default void foo(@Nullable int var1, @NotNull String var2) {
        ((TestAnnotations)this).foo(var1, var2);
    }

    @Nullable
    default String nullable() {
        return ((TestAnnotations)this).nullable();
    }

    default ITestInterface foo() {
        return ((TestAnnotations)this).foo();
    }

    default ITestOtherClass boz(ITestOtherClass var1) {
        return ((TestAnnotations)this).boz((TestOtherClass)var1);
    }

    @Nullable
    final default String getInstanceField() {
        return ((TestAnnotations)this).instanceField;
    }

    final default void setInstanceField(@Nullable String var1) {
        ((TestAnnotations)this).instanceField = var1;
    }

    @Nullable
    static final String getStaticField() {
        return TestAnnotations.staticField;
    }

    static final void setStaticField(@Nullable String var0) {
        TestAnnotations.staticField = var0;
    }

    @NotNull
    static ITestAnnotations[] array(int var0) {
        return new TestAnnotations[var0];
    }
}
