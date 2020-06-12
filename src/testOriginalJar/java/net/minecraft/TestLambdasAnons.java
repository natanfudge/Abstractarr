package net.minecraft;

public class TestLambdasAnons {
    public static void foo() {
        TestInterface x = new TestInterface() {
            @Override
            public TestInterface foo() {
                return null;
            }
        };

        TestInterface y = () -> null;
    }
}
