package v1.net.minecraft;

import net.minecraft.TestAbstractClass;
import net.minecraft.TestAbstractImpl;
import net.minecraft.TestConcreteClass;
import net.minecraft.TestOtherClass;
import org.jetbrains.annotations.NotNull;

public final interface ITestAbstractImpl extends ITestInterface, Comparable<ITestAbstractImpl>, ITestAbstractClass {
    @NotNull
    static ITestAbstractImpl create(int var0, ITestAbstractClass var1) {
        return new TestAbstractImpl(var0, (TestAbstractClass)var1);
    }

    default ITestAbstractClass abstractMethod() {
        return ((TestAbstractImpl)this).abstractMethod();
    }

    default void someImplMethodWithArg(ITestOtherClass var1) {
        ((TestAbstractImpl)this).someImplMethodWithArg((TestOtherClass)var1);
    }

    default ITestAbstractClass abstractMethodParam(ITestConcreteClass var1) {
        return ((TestAbstractImpl)this).abstractMethodParam((TestConcreteClass)var1);
    }

    default ITestInterface foo() {
        return ((TestAbstractImpl)this).foo();
    }

    default ITestOtherClass boz(ITestOtherClass var1) {
        return ((TestAbstractImpl)this).boz((TestOtherClass)var1);
    }

    default int compareTo(ITestAbstractImpl var1) {
        return ((TestAbstractImpl)this).compareTo((TestAbstractImpl)var1);
    }

    static void testStaticParam(ITestOtherClass var0) {
        TestAbstractImpl.testStaticParam((TestOtherClass)var0);
    }

    @NotNull
    static ITestAbstractImpl[] array(int var0) {
        return new TestAbstractImpl[var0];
    }
}
