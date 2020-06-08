package v1.net.minecraft;

import febb.apiruntime.SuperTyped;
import net.minecraft.TestFinalClass;
import net.minecraft.TestOtherClass;
import net.minecraft.TestSuperClass;

public interface ITestFinalClass extends SuperTyped<TestSuperClass> {
  ITestOtherClass publicStaticOtherClassField = (ITestOtherClass)(Object)TestFinalClass.publicStaticOtherClassField;

  default int publicInt(ITestOtherClass someClass) {
    return ((TestFinalClass)(Object)this).publicInt((TestOtherClass)(Object)someClass);
  }

  default int publicFinalInt() {
    return ((TestFinalClass)(Object)this).publicFinalInt();
  }

  default ITestOtherClass getPublicField() {
    return (ITestOtherClass)(Object)((TestFinalClass)(Object)this).publicField;
  }

  default void setPublicField(ITestOtherClass publicField) {
    ((TestFinalClass)(Object)this).publicField = (TestOtherClass)(Object)publicField;
  }

  default int getPublicFinalField() {
    return ((TestFinalClass)(Object)this).publicFinalField;
  }

  static String getPublicStaticField() {
    return TestFinalClass.publicStaticField;
  }

  static void setPublicStaticField(String publicStaticField) {
    TestFinalClass.publicStaticField = publicStaticField;
  }
}
