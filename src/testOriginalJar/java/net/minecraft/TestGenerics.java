package net.minecraft;

import java.util.ArrayList;
import java.util.List;

public class TestGenerics<T1 extends ArrayList<TestConcreteClass>
        ,T2 extends T1, T3 extends List<T2>, T4> extends ArrayList<T3> implements List<T3>,
        Comparable<TestInterface>, ExtendedInterface<TestAbstractImpl>, ExtendedInterfaceRaw
{
    @Override
    public int compareTo(TestInterface o) {
        return 2;
    }

    public static class Extendor<T extends ArrayList<TestConcreteClass>>
            extends TestGenerics<T, T, List<T>, SomethingToInsert> {
    }

    public abstract static class SomethingToInsert implements List<Boolean>, Comparable<String>  {

        @Override
        public int compareTo(String o) {
            return 0;
        }
    }

    public <T extends T2> T genericMethod(T param1, T2 param2, List<String> param3, List<TestAbstractClass> param4,
                                          List<? extends TestAbstractClass> param5, List<? super TestAbstractImpl> param6, List<?> param7,TestOtherClass param8) {
        return null;
    }


    public class SomeInnerClass<T> {
        public T useInnerClassGeneric() {
            return null;
        }

        public T1 useOuterClassGeneric() {
            return null;
        }
    }

    public T1 genericField1;
    public List<?> genericField2;
    public List<? extends T2> genericField3;
    public List<? super T1> genericField4;
    public List<TestInterface> genericField5;
}
