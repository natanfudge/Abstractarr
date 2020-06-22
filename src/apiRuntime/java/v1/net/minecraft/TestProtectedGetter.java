package v1.net.minecraft;

import net.minecraft.TestProtected;

public class TestProtectedGetter  extends TestProtected {
    protected String getThing(){
        return this.instance;
    }
}
