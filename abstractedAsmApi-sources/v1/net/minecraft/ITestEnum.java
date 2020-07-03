package v1.net.minecraft;

import net.minecraft.TestEnum;
import org.jetbrains.annotations.NotNull;

public final interface ITestEnum {
    ITestEnum THING = TestEnum.THING;
    ITestEnum THING2 = TestEnum.THING2;

    default Enum<TestEnum> asSuper() {
        return (Enum)this;
    }

    static ITestEnum[] values() {
        return TestEnum.values();
    }

    static ITestEnum valueOf(String var0) {
        return TestEnum.valueOf(var0);
    }

    static ITestEnum foo() {
        return TestEnum.foo();
    }

    default ITestAbstractClass bar() {
        return ((TestEnum)this).bar();
    }

    final default int getX() {
        return ((TestEnum)this).x;
    }

    final default void setX(int var1) {
        ((TestEnum)this).x = var1;
    }

    @NotNull
    static ITestEnum[] array(int var0) {
        return new TestEnum[var0];
    }
}
