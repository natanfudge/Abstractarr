package net.minecraft;

public class TestLambdasAnons {
    public static void foo() {
        TestLambdaInterface x = new TestLambdaInterface() {
            @Override
            public TestLambdaInterface foo() {
                return null;
            }
        };

        TestLambdaInterface y = () -> null;
    }
}
