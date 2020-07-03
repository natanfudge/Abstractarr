package v1.net.minecraft;

import net.minecraft.TestInterface;
import net.minecraft.TestOtherClass;
import org.jetbrains.annotations.NotNull;

public final interface ITestInterface {
    int x = TestInterface.x;

    ITestInterface foo();

    default int bar() {
        return ((TestInterface)this).bar();
    }

    default ITestOtherClass baz() {
        return ((TestInterface)this).baz();
    }

    default ITestOtherClass baz(ITestOtherClass var1) {
        return ((TestInterface)this).baz((TestOtherClass)var1);
    }

    ITestOtherClass boz(ITestOtherClass var1);

    static void testStatic() {
        TestInterface.testStatic();
    }

    static void testStaticParam(ITestOtherClass var0) {
        TestInterface.testStaticParam((TestOtherClass)var0);
    }

    @NotNull
    static ITestInterface[] array(int var0) {
        return new TestInterface[var0];
    }
}
