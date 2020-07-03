package v1.net.minecraft;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseTestGenerics<T1 extends ArrayList<ITestConcreteClass>, T2 extends T1, T3 extends List<T2>, T4> extends ArrayList<T3> implements ITestGenerics<T1, T2, T3, T4>, List<T3>, Comparable<ITestInterface>, IExtendedInterface<ITestAbstractImpl>, IExtendedInterfaceRaw {
    public abstract static class Extendor<T extends ArrayList<ITestConcreteClass>> extends ArrayList<List<T>> implements ITestGenerics.Extendor<T> {
    }

    public abstract static class SomethingToInsert implements ITestGenerics.SomethingToInsert, List<Boolean>, Comparable<String> {
    }
}
