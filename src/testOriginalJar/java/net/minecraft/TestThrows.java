package net.minecraft;

public class TestThrows {
    public TestThrows() throws RuntimeException {
        throw new RuntimeException();
    }

    public void foo() throws NullPointerException {
        throw new NullPointerException();
    }
}
