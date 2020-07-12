package net.minecraft;

public class TestClashingNamesSuper {
    public TestOtherClass getSuperWillFuckUp() {
        return new TestOtherClass();
    }
}
