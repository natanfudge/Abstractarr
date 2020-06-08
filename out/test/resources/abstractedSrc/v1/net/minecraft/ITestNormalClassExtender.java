package v1.net.minecraft;

import febb.apiruntime.SuperTyped;
import java.util.ArrayList;
import net.minecraft.TestNormalClassExtender;

public interface ITestNormalClassExtender extends SuperTyped<ArrayList> {
  static ITestNormalClassExtender create() {
    return (ITestNormalClassExtender)new TestNormalClassExtender();
  }

  default String foo() {
    return ((TestNormalClassExtender)this).foo();
  }
}
