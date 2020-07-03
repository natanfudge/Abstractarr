package v1.net.minecraft;

import net.minecraft.TestOtherClass;
import net.minecraft.TestProtected;

public abstract class BaseTestProtected implements ITestProtected {
    protected static final String constant = TestProtected.constant;

    protected void fooNoMc() {
        super.fooNoMc();
    }

    protected void fooMc(ITestOtherClass var1) {
        super.fooMc((TestOtherClass)var1);
    }

    protected static void staticNoMc() {
        TestProtected.staticNoMc();
    }

    protected static void staticMc(ITestOtherClass var0) {
        TestProtected.staticMc((TestOtherClass)var0);
    }

    protected final void fooNoMcFinal() {
        super.fooNoMcFinal();
    }

    protected final void fooMcFinal(ITestOtherClass var1) {
        super.fooMcFinal((TestOtherClass)var1);
    }

    protected static final String getStaticField() {
        return TestProtected.staticField;
    }

    protected static final void setStaticField(String var0) {
        TestProtected.staticField = var0;
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
