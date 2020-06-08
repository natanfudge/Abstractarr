package v1.net.minecraft;

import febb.apiruntime.SuperTyped;
import net.minecraft.TestConcreteClass;
import net.minecraft.TestOtherClass;
import net.minecraft.TestSuperClass;

public interface ITestConcreteClass extends SuperTyped<TestSuperClass> {
  ITestOtherClass publicStaticOtherClassField = (ITestOtherClass)TestConcreteClass.publicStaticOtherClassField;

  String publicStaticFinalField = TestConcreteClass.publicStaticFinalField;

  default int publicInt() {
    return ((TestConcreteClass)this).publicInt();
  }

  static int publicStatic() {
    return TestConcreteClass.publicStatic();
  }

  default int mutatesField() {
    return ((TestConcreteClass)this).mutatesField();
  }

  default int finalMethod() {
    return ((TestConcreteClass)this).finalMethod();
  }

  default TestStaticInnerClass innerClassMethod() {
    return (TestStaticInnerClass)((TestConcreteClass)this).innerClassMethod();
  }

  static ITestConcreteClass create(int arg1, ITestOtherClass arg2) {
    return (ITestConcreteClass)new TestConcreteClass(arg1, (TestOtherClass)(Object)arg2);
  }

  default int getPublicField() {
    return ((TestConcreteClass)this).publicField;
  }

  default void setPublicField(int publicField) {
    ((TestConcreteClass)this).publicField = publicField;
  }

  default int getPublicFinalField() {
    return ((TestConcreteClass)this).publicFinalField;
  }

  default ITestOtherClass getOtherClassField() {
    return (ITestOtherClass)((TestConcreteClass)this).otherClassField;
  }

  default void setOtherClassField(ITestOtherClass otherClassField) {
    ((TestConcreteClass)this).otherClassField = (TestOtherClass)otherClassField;
  }

  static String getPublicStaticField() {
    return TestConcreteClass.publicStaticField;
  }

  static void setPublicStaticField(String publicStaticField) {
    TestConcreteClass.publicStaticField = publicStaticField;
  }

  default TestInnerClass newTestInnerClass(int arg1, ITestOtherClass arg2) {
    return (TestInnerClass)((TestConcreteClass)this).new TestInnerClass(arg1, (TestOtherClass)(Object)arg2);
  }

  interface TestInnerClass {
    String publicStaticFinalField = TestConcreteClass.TestInnerClass.publicStaticFinalField;

    default int publicInt() {
      return ((TestConcreteClass.TestInnerClass)this).publicInt();
    }

    default int mutatesField() {
      return ((TestConcreteClass.TestInnerClass)this).mutatesField();
    }

    default int finalMethod() {
      return ((TestConcreteClass.TestInnerClass)this).finalMethod();
    }

    default int getPublicField() {
      return ((TestConcreteClass.TestInnerClass)this).publicField;
    }

    default void setPublicField(int publicField) {
      ((TestConcreteClass.TestInnerClass)this).publicField = publicField;
    }

    default int getPublicFinalField() {
      return ((TestConcreteClass.TestInnerClass)this).publicFinalField;
    }

    default ITestOtherClass getOtherClassField() {
      return (ITestOtherClass)((TestConcreteClass.TestInnerClass)this).otherClassField;
    }

    default void setOtherClassField(ITestOtherClass otherClassField) {
      ((TestConcreteClass.TestInnerClass)this).otherClassField = (TestOtherClass)otherClassField;
    }
  }

  interface TestStaticInnerClass extends SuperTyped<TestSuperClass> {
    ITestOtherClass publicStaticOtherClassField = (ITestOtherClass)TestConcreteClass.TestStaticInnerClass.publicStaticOtherClassField;

    String publicStaticFinalField = TestConcreteClass.TestStaticInnerClass.publicStaticFinalField;

    static int publicStatic() {
      return TestConcreteClass.TestStaticInnerClass.publicStatic();
    }

    static TestStaticInnerClass create(int arg1, ITestOtherClass arg2) {
      return (TestStaticInnerClass)new TestConcreteClass.TestStaticInnerClass(arg1, (TestOtherClass)(Object)arg2);
    }

    default int publicInt() {
      return ((TestConcreteClass.TestStaticInnerClass)this).publicInt();
    }

    default int mutatesField() {
      return ((TestConcreteClass.TestStaticInnerClass)this).mutatesField();
    }

    default int finalMethod() {
      return ((TestConcreteClass.TestStaticInnerClass)this).finalMethod();
    }

    static String getPublicStaticField() {
      return TestConcreteClass.TestStaticInnerClass.publicStaticField;
    }

    static void setPublicStaticField(String publicStaticField) {
      TestConcreteClass.TestStaticInnerClass.publicStaticField = publicStaticField;
    }

    default int getPublicField() {
      return ((TestConcreteClass.TestStaticInnerClass)this).publicField;
    }

    default void setPublicField(int publicField) {
      ((TestConcreteClass.TestStaticInnerClass)this).publicField = publicField;
    }

    default int getPublicFinalField() {
      return ((TestConcreteClass.TestStaticInnerClass)this).publicFinalField;
    }

    default ITestOtherClass getOtherClassField() {
      return (ITestOtherClass)((TestConcreteClass.TestStaticInnerClass)this).otherClassField;
    }

    default void setOtherClassField(ITestOtherClass otherClassField) {
      ((TestConcreteClass.TestStaticInnerClass)this).otherClassField = (TestOtherClass)otherClassField;
    }
  }
}
