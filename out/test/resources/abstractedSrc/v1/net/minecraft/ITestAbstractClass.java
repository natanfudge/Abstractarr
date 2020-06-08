package v1.net.minecraft;

import net.minecraft.TestAbstractClass;
import net.minecraft.TestConcreteClass;

public interface ITestAbstractClass {
  default ITestAbstractClass abstractMethod() {
    return (ITestAbstractClass)((TestAbstractClass)this).abstractMethod();
  }

  default ITestAbstractClass abstractMethodParam(ITestConcreteClass x) {
    return (ITestAbstractClass)((TestAbstractClass)this).abstractMethodParam((TestConcreteClass)(Object)x);
  }

  default int getField() {
    return ((TestAbstractClass)this).field;
  }

  default void setField(int field) {
    ((TestAbstractClass)this).field = field;
  }
}
