package v1.net.minecraft;

import java.util.ArrayList;

public abstract class BaseTestNormalClassExtender extends ArrayList<String> implements ITestNormalClassExtender {
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
