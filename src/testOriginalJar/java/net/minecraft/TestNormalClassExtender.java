package net.minecraft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

public class TestNormalClassExtender extends ArrayList<String> {
    public String foo() {
        return "123";
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return super.containsAll(c);
    }

    @Override
    public Stream<String> stream() {
        return null;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
