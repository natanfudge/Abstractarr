package v1.net.minecraft;

import net.minecraft.TestOtherClass;
import net.minecraft.TestSuperClass;

public interface ITestSuperClass {
  static ITestSuperClass create(ITestOtherClass otherParam) {
    return (ITestSuperClass)new TestSuperClass((TestOtherClass)(Object)otherParam);
  }

  default int inheritedMethod() {
    return ((TestSuperClass)this).inheritedMethod();
  }

  default int overridenMethod() {
    return ((TestSuperClass)this).overridenMethod();
  }

  default String getInheritedField() {
    return ((TestSuperClass)this).inheritedField;
  }

  default void setInheritedField(String inheritedField) {
    ((TestSuperClass)this).inheritedField = inheritedField;
  }

  static String getStaticField() {
    return TestSuperClass.staticField;
  }

  static void setStaticField(String staticField) {
    TestSuperClass.staticField = staticField;
  }
}
