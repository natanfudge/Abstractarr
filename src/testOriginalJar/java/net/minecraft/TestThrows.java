package net.minecraft;

import java.io.IOException;

public class TestThrows {
    public TestThrows() throws RuntimeException {
        throw new RuntimeException();
    }

    public void foo() throws NullPointerException {
        throw new NullPointerException();
    }

    public void checked() throws IOException {

    }

    public <T extends Throwable> void bar() throws T {

    }
}
