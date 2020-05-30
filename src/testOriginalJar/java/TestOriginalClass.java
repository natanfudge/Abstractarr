public class TestOriginalClass {
    private int instanceField;
    public static String staticField;

    private void privateVoid() {
        instanceField = 7;
    }

    public int publicInt() {
        return 2;
    }

    protected static String protectedStatic() {
        return "Original";
    }

    int packageArgs(int arg1, int arg2) {
        return arg1 + arg2;
    }

    public int notOverwritten() {
        instanceField++;
        return 123;
    }

}
