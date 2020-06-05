package net.minecraft;


public interface TestInterface  {
    TestInterface foo();

    default int bar() {
        return 2;
    }

    int x = 2;
}
