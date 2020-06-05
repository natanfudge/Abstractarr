package net.minecraft;


public abstract class TestAbstractClass {
    public int field;

    public abstract TestAbstractClass abstractMethod();
    public abstract TestAbstractClass abstractMethodParam(TestConcreteClass x);

    public TestAbstractClass(int x1, TestAbstractClass x2){}
}
