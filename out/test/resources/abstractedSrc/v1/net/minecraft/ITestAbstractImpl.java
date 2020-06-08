package v1.net.minecraft;

import febb.apiruntime.SuperTyped;
import net.minecraft.TestAbstractClass;
import net.minecraft.TestAbstractImpl;

public interface ITestAbstractImpl extends ITestInterface, Comparable, SuperTyped<TestAbstractClass> {
  default int compareTo(ITestAbstractImpl o) {
    return ((TestAbstractImpl)this).compareTo((TestAbstractImpl)(Object)o);
  }
}
