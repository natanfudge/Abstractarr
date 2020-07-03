package v1.net.minecraft;

import net.minecraft.TestClashingNames;
import org.jetbrains.annotations.NotNull;

public final interface ITestClashingNames {
    @NotNull
    static ITestClashingNames create() {
        return new TestClashingNames();
    }

    default boolean isSomeBool() {
        return ((TestClashingNames)this).isSomeBool();
    }

    default String getSomeString() {
        return ((TestClashingNames)this).getSomeString();
    }

    default int getSomeInt(int var1) {
        return ((TestClashingNames)this).getSomeInt(var1);
    }

    final default boolean isSomeBool_field() {
        return ((TestClashingNames)this).someBool;
    }

    final default void setSomeBool(boolean var1) {
        ((TestClashingNames)this).someBool = var1;
    }

    final default String getSomeString_field() {
        return ((TestClashingNames)this).someString;
    }

    final default void setSomeString(String var1) {
        ((TestClashingNames)this).someString = var1;
    }

    final default int getSomeInt() {
        return ((TestClashingNames)this).someInt;
    }

    final default void setSomeInt(int var1) {
        ((TestClashingNames)this).someInt = var1;
    }

    @NotNull
    static ITestClashingNames[] array(int var0) {
        return new TestClashingNames[var0];
    }
}
