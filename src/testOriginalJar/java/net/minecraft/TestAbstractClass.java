package net.minecraft;

import v1.net.minecraft.ITestAbstractClass;

public abstract class TestAbstractClass implements ITestAbstractClass {
    public int field;

    public abstract TestAbstractClass abstractMethod();
    public abstract TestAbstractClass abstractMethodParam(TestConcreteClass x);

    public TestAbstractClass(int x1, TestAbstractClass x2){}
}
