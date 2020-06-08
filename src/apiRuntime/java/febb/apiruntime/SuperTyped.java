package febb.apiruntime;

import java.util.ArrayList;

public interface SuperTyped<T> {
    default T asSuper() {
        return (T) this;
    }

//    class X implements SuperTyped<ArrayList<String>>
}
