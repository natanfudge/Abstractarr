package v1.net.minecraft;

import net.minecraft.TestConcreteClass;
import net.minecraft.TestOtherClass;
import org.jetbrains.annotations.NotNull;

public final interface ITestConcreteClass extends ITestSuperClass {
    ITestOtherClass publicStaticOtherClassField = TestConcreteClass.publicStaticOtherClassField;
    String publicStaticFinalField = TestConcreteClass.publicStaticFinalField;

    /**
     * Reads a {@link UUID} from its NBT representation in this {@code CompoundTag}.
     */
    default int publicInt(ITestOtherClass var1) {
        return ((TestConcreteClass)this).publicInt((TestOtherClass)var1);
    }

    static int publicStatic() {
        return TestConcreteClass.publicStatic();
    }

    default int mutatesField() {
        return ((TestConcreteClass)this).mutatesField();
    }

    final default int finalMethod() {
        return ((TestConcreteClass)this).finalMethod();
    }

    default ITestConcreteClass.TestStaticInnerClass innerClassMethod() {
        return ((TestConcreteClass)this).innerClassMethod();
    }

    @NotNull
    static ITestConcreteClass create(int var0, ITestOtherClass var1) {
        return new TestConcreteClass(var0, (TestOtherClass)var1);
    }

    @NotNull
    static ITestConcreteClass create() {
        return new TestConcreteClass();
    }

    final default int getPublicField() {
        return ((TestConcreteClass)this).publicField;
    }

    final default void setPublicField(int var1) {
        ((TestConcreteClass)this).publicField = var1;
    }

    final default int getPublicFinalField() {
        return ((TestConcreteClass)this).publicFinalField;
    }

    final default boolean isSomeBool() {
        return ((TestConcreteClass)this).someBool;
    }

    final default void setSomeBool(boolean var1) {
        ((TestConcreteClass)this).someBool = var1;
    }

    final default boolean isSomeOtherBool() {
        return ((TestConcreteClass)this).isSomeOtherBool;
    }

    final default void setIsSomeOtherBool(boolean var1) {
        ((TestConcreteClass)this).isSomeOtherBool = var1;
    }

    final default ITestOtherClass getOtherClassField() {
        return ((TestConcreteClass)this).otherClassField;
    }

    final default void setOtherClassField(ITestOtherClass var1) {
        ((TestConcreteClass)this).otherClassField = (TestOtherClass)var1;
    }

    static final String getPublicStaticField() {
        return TestConcreteClass.publicStaticField;
    }

    static final void setPublicStaticField(String var0) {
        TestConcreteClass.publicStaticField = var0;
    }

    @NotNull
    final default ITestConcreteClass.TestInnerClass newTestInnerClass(int var1, ITestOtherClass var2) throws RuntimeException {
        return new TestConcreteClass.TestInnerClass((TestConcreteClass)this, var1, (TestOtherClass)var2);
    }

    @NotNull
    static ITestConcreteClass[] array(int var0) {
        return new TestConcreteClass[var0];
    }

    public final interface TestInnerClass {
        String publicStaticFinalField = TestConcreteClass.TestInnerClass.publicStaticFinalField;

        default int publicInt() {
            return ((TestConcreteClass.TestInnerClass)this).publicInt();
        }

        default int mutatesField() {
            return ((TestConcreteClass.TestInnerClass)this).mutatesField();
        }

        final default int finalMethod() {
            return ((TestConcreteClass.TestInnerClass)this).finalMethod();
        }

        final default int getPublicField() {
            return ((TestConcreteClass.TestInnerClass)this).publicField;
        }

        final default void setPublicField(int var1) {
            ((TestConcreteClass.TestInnerClass)this).publicField = var1;
        }

        final default int getPublicFinalField() {
            return ((TestConcreteClass.TestInnerClass)this).publicFinalField;
        }

        final default ITestOtherClass getOtherClassField() {
            return ((TestConcreteClass.TestInnerClass)this).otherClassField;
        }

        final default void setOtherClassField(ITestOtherClass var1) {
            ((TestConcreteClass.TestInnerClass)this).otherClassField = (TestOtherClass)var1;
        }

        @NotNull
        static ITestConcreteClass.TestInnerClass[] array(int var0) {
            return new TestConcreteClass.TestInnerClass[var0];
        }
    }

    public final interface TestStaticInnerClass extends ITestSuperClass {
        ITestOtherClass publicStaticOtherClassField = TestConcreteClass.TestStaticInnerClass.publicStaticOtherClassField;
        String publicStaticFinalField = TestConcreteClass.TestStaticInnerClass.publicStaticFinalField;

        static int publicStatic() {
            return TestConcreteClass.TestStaticInnerClass.publicStatic();
        }

        @NotNull
        static ITestConcreteClass.TestStaticInnerClass create(int var0, ITestOtherClass var1) {
            return new TestConcreteClass.TestStaticInnerClass(var0, (TestOtherClass)var1);
        }

        default int publicInt() {
            return ((TestConcreteClass.TestStaticInnerClass)this).publicInt();
        }

        default int mutatesField() {
            return ((TestConcreteClass.TestStaticInnerClass)this).mutatesField();
        }

        final default int finalMethod() {
            return ((TestConcreteClass.TestStaticInnerClass)this).finalMethod();
        }

        static final String getPublicStaticField() {
            return TestConcreteClass.TestStaticInnerClass.publicStaticField;
        }

        static final void setPublicStaticField(String var0) {
            TestConcreteClass.TestStaticInnerClass.publicStaticField = var0;
        }

        final default int getPublicField() {
            return ((TestConcreteClass.TestStaticInnerClass)this).publicField;
        }

        final default void setPublicField(int var1) {
            ((TestConcreteClass.TestStaticInnerClass)this).publicField = var1;
        }

        final default int getPublicFinalField() {
            return ((TestConcreteClass.TestStaticInnerClass)this).publicFinalField;
        }

        final default ITestOtherClass getOtherClassField() {
            return ((TestConcreteClass.TestStaticInnerClass)this).otherClassField;
        }

        final default void setOtherClassField(ITestOtherClass var1) {
            ((TestConcreteClass.TestStaticInnerClass)this).otherClassField = (TestOtherClass)var1;
        }

        @NotNull
        static ITestConcreteClass.TestStaticInnerClass[] array(int var0) {
            return new TestConcreteClass.TestStaticInnerClass[var0];
        }
    }
}
