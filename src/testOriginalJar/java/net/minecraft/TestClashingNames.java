package net.minecraft;

public class TestClashingNames {
    public boolean someBool;

    public boolean isSomeBool() {
        return false;
    }

    public String someString = "string";

    public String getSomeString() {
        return "shit";
    }

    public int someInt;

    public int getSomeInt(int param) {
        return 0;
    }

    public void setSomeInt(int param) {
        someInt = param;
    }

    public TestClashingNames(int x) {

    }

    public static TestClashingNames create(int x) {
        return new TestClashingNames(x);
    }
}
