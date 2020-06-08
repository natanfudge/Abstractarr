package v1.net.minecraft;

import net.minecraft.TestConcreteClass;
import net.minecraft.TestFinalClass;
import net.minecraft.TestOtherClass;

public interface ITestOtherClass {
  static ITestOtherClass create() {
    return (ITestOtherClass)new TestOtherClass();
  }

  default void oneCastTest(ITestConcreteClass x) {
    ((TestOtherClass)this).oneCastTest((TestConcreteClass)(Object)x);
  }

  default void twoCastTest(ITestConcreteClass x) {
    ((TestOtherClass)this).twoCastTest((TestConcreteClass)(Object)x);
  }

  default void realFinalCastTest(ITestFinalClass x) {
    ((TestOtherClass)this).realFinalCastTest((TestFinalClass)(Object)x);
  }
}
