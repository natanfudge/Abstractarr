package net.minecraft;

import java.util.List;

public class TestOverrideReturnTypeChangeSuper {
    public Object foo() {

        return new Object();
    }

    public List<TestOtherClass> bar() {
        return null;
    }

    public TestAbstractClass mcClass(){
        return null;
    }
}
