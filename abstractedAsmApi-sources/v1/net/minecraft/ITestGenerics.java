package v1.net.minecraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.TestGenerics;
import net.minecraft.TestInterface;
import net.minecraft.TestOtherClass;
import org.jetbrains.annotations.NotNull;

public final interface ITestGenerics<T1 extends ArrayList<ITestConcreteClass>, T2 extends T1, T3 extends List<T2>, T4> extends List<T3>, Comparable<ITestInterface>, IExtendedInterface<ITestAbstractImpl>, IExtendedInterfaceRaw {
    default ArrayList<T3> asSuper() {
        return (ArrayList)this;
    }

    @NotNull
    static <T1 extends ArrayList<ITestConcreteClass>, T2 extends T1, T3 extends List<T2>, T4> ITestGenerics<T1, T2, T3, T4> create() {
        return new TestGenerics();
    }

    default int compareTo(ITestInterface var1) {
        return ((TestGenerics)this).compareTo((TestInterface)var1);
    }

    default <T extends T2> T genericMethod(T var1, T2 var2, List<String> var3, List<ITestAbstractClass> var4, List<? extends ITestAbstractClass> var5, List<? super ITestAbstractImpl> var6, List<?> var7, ITestOtherClass var8) {
        return ((TestGenerics)this).genericMethod((T)var1, var2, var3, var4, var5, var6, var7, (TestOtherClass)var8);
    }

    final default T1 getGenericField1() {
        return ((TestGenerics)this).genericField1;
    }

    final default void setGenericField1(T1 var1) {
        ((TestGenerics)this).genericField1 = var1;
    }

    final default List<?> getGenericField2() {
        return ((TestGenerics)this).genericField2;
    }

    final default void setGenericField2(List<?> var1) {
        ((TestGenerics)this).genericField2 = var1;
    }

    final default List<? extends T2> getGenericField3() {
        return ((TestGenerics)this).genericField3;
    }

    final default void setGenericField3(List<? extends T2> var1) {
        ((TestGenerics)this).genericField3 = var1;
    }

    final default List<? super T1> getGenericField4() {
        return ((TestGenerics)this).genericField4;
    }

    final default void setGenericField4(List<? super T1> var1) {
        ((TestGenerics)this).genericField4 = var1;
    }

    final default List<ITestInterface> getGenericField5() {
        return ((TestGenerics)this).genericField5;
    }

    final default void setGenericField5(List<ITestInterface> var1) {
        ((TestGenerics)this).genericField5 = var1;
    }

    @NotNull
    final default <T> ITestGenerics.SomeInnerClass<T1, T2, T3, T4, T> newSomeInnerClass() {
        return new TestGenerics.SomeInnerClass((TestGenerics)this);
    }

    @NotNull
    static <T1 extends ArrayList<ITestConcreteClass>, T2 extends T1, T3 extends List<T2>, T4> ITestGenerics<T1, T2, T3, T4>[] array(int var0) {
        return new TestGenerics[var0];
    }

    public final interface Extendor<T extends ArrayList<ITestConcreteClass>> extends ITestGenerics<T, T, List<T>, ITestGenerics.SomethingToInsert> {
        @NotNull
        static <T extends ArrayList<ITestConcreteClass>> ITestGenerics.Extendor<T> create() {
            return new TestGenerics.Extendor();
        }

        @NotNull
        static <T extends ArrayList<ITestConcreteClass>> ITestGenerics.Extendor<T>[] array(int var0) {
            return new TestGenerics.Extendor[var0];
        }
    }

    public final interface SomeInnerClass<T1 extends ArrayList<ITestConcreteClass>, T2 extends T1, T3 extends List<T2>, T4, T> {
        default T useInnerClassGeneric() {
            return ((TestGenerics.SomeInnerClass)this).useInnerClassGeneric();
        }

        default T1 useOuterClassGeneric() {
            return (T1)((TestGenerics.SomeInnerClass)this).useOuterClassGeneric();
        }

        @NotNull
        static <T1 extends ArrayList<ITestConcreteClass>, T2 extends T1, T3 extends List<T2>, T4, T> ITestGenerics.SomeInnerClass<T1, T2, T3, T4, T>[] array(int var0) {
            return new TestGenerics.SomeInnerClass[var0];
        }
    }

    public final interface SomethingToInsert extends List<Boolean>, Comparable<String> {
        default int compareTo(String var1) {
            return ((TestGenerics.SomethingToInsert)this).compareTo(var1);
        }

        @NotNull
        static ITestGenerics.SomethingToInsert[] array(int var0) {
            return new TestGenerics.SomethingToInsert[var0];
        }
    }
}
