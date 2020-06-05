package net.minecraft;


public class TestAbstractImpl extends TestAbstractClass implements TestInterface, Comparable<TestAbstractImpl> {
    public TestAbstractImpl(int x1, TestAbstractClass x2) {
        super(x1, x2);
    }

    @Override
    public TestAbstractClass abstractMethod() {
        return new TestAbstractImpl(1, null);
    }

    @Override
    public TestAbstractClass abstractMethodParam(TestConcreteClass x) {
        return new TestAbstractImpl(2, new TestAbstractImpl(3, null));
    }

    @Override
    public TestInterface foo() {
        return null;
    }

    @Override
    public int compareTo(TestAbstractImpl o) {
        return 0;
    }
}
