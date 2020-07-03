package v1.net.minecraft;

import net.minecraft.TestLambdasAnons;
import org.jetbrains.annotations.NotNull;

public final interface ITestLambdasAnons {
    @NotNull
    static ITestLambdasAnons create() {
        return new TestLambdasAnons();
    }

    static void foo() {
        TestLambdasAnons.foo();
    }

    @NotNull
    static ITestLambdasAnons[] array(int var0) {
        return new TestLambdasAnons[var0];
    }
}
