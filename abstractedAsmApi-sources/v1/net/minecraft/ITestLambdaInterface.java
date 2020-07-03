package v1.net.minecraft;

import net.minecraft.TestLambdaInterface;
import org.jetbrains.annotations.NotNull;

public final interface ITestLambdaInterface {
    default ITestLambdaInterface foo() {
        return ((TestLambdaInterface)this).foo();
    }

    @NotNull
    static ITestLambdaInterface[] array(int var0) {
        return new TestLambdaInterface[var0];
    }
}
