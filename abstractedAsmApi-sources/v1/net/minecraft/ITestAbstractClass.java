package v1.net.minecraft;

import net.minecraft.TestAbstractClass;
import net.minecraft.TestOtherClass;
import org.jetbrains.annotations.NotNull;

public final interface ITestAbstractClass extends ITestInterface {
    ITestAbstractClass abstractMethod();

    ITestAbstractClass abstractMethodParam(ITestConcreteClass var1);

    static void testStatic() {
        TestAbstractClass.testStatic();
    }

    static void testStaticParam(ITestOtherClass var0) {
        TestAbstractClass.testStaticParam((TestOtherClass)var0);
    }

    final default int getField() {
        return ((TestAbstractClass)this).field;
    }

    final default void setField(int var1) {
        ((TestAbstractClass)this).field = var1;
    }

    @NotNull
    static ITestAbstractClass[] array(int var0) {
        return new TestAbstractClass[var0];
    }
}
