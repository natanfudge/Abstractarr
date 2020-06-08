package v1.net.minecraft;

import febb.apiruntime.SuperTyped;
import net.minecraft.TestConcreteClass;
import net.minecraft.TestInnerExtender;

public interface ITestInnerExtender extends SuperTyped<TestConcreteClass.TestStaticInnerClass> {
  default void normalMethod() {
    ((TestInnerExtender)this).normalMethod();
  }
}
