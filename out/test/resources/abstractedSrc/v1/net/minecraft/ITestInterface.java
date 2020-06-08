package v1.net.minecraft;

import net.minecraft.TestInterface;

public interface ITestInterface {
  int x = TestInterface.x;

  default ITestInterface foo() {
    return (ITestInterface)((TestInterface)this).foo();
  }

  default int bar() {
    return ((TestInterface)this).bar();
  }
}
