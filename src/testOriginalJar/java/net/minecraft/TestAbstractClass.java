package net.minecraft;


public abstract class TestAbstractClass implements TestInterface {
    public int field;

    public abstract TestAbstractClass abstractMethod();
    public abstract TestAbstractClass abstractMethodParam(TestConcreteClass x);

    public TestAbstractClass(int x1, TestAbstractClass x2){}

    @Override
    public TestOtherClass baz() {
        return null;
    }
}
