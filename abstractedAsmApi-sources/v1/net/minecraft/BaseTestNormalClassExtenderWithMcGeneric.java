package v1.net.minecraft;

import java.util.ArrayList;

public abstract class BaseTestNormalClassExtenderWithMcGeneric extends ArrayList<ITestOtherClass> implements ITestNormalClassExtenderWithMcGeneric {
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
