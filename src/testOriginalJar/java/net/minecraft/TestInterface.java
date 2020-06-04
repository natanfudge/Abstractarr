package net.minecraft;

import v1.net.minecraft.ITestInterface;

public interface TestInterface extends ITestInterface {
    TestInterface foo();

    default int bar() {
        return 2;
    }

    int x = 2;
}
