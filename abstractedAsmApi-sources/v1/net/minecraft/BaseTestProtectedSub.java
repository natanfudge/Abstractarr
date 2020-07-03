package v1.net.minecraft;

import net.minecraft.TestOtherClass;
import net.minecraft.TestProtectedSub;

public abstract class BaseTestProtectedSub implements ITestProtectedSub {
    protected static final String constant = TestProtectedSub.constant;

    protected void fooNoMc() {
        super.fooNoMc();
    }

    protected void fooMc(ITestOtherClass var1) {
        super.fooMc((TestOtherClass)var1);
    }

    protected static void staticNoMc() {
        TestProtectedSub.staticNoMc();
    }

    protected static void staticMc(ITestOtherClass var0) {
        TestProtectedSub.staticMc((TestOtherClass)var0);
    }

    protected void newMethod(ITestOtherClass var1) {
        super.newMethod((TestOtherClass)var1);
    }

    protected final void fooNoMcFinal() {
        super.fooNoMcFinal();
    }

    protected final void fooMcFinal(ITestOtherClass var1) {
        super.fooMcFinal((TestOtherClass)var1);
    }

    protected static final String getStaticField() {
        return TestProtectedSub.staticField;
    }

    protected static final void setStaticField(String var0) {
        TestProtectedSub.staticField = var0;
    }

    protected final String getInstanceFinal() {
        return super.instanceFinal;
    }

    protected final String getInstance() {
        return super.instance;
    }

    protected final void setInstance(String var1) {
        super.instance = var1;
    }
}
