package net.minecraft;

public class TestArrays {
    public TestConcreteClass[] arrField;

    public TestConcreteClass[] arrMethod() {
        TestConcreteClass[] arr = new TestConcreteClass[3];
        arr[1] = new TestConcreteClass(0, null);
        return arr;
    }

    public void arrParam(TestConcreteClass[] arr) {
    }
}
