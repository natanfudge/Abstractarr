package febb.apiruntime;

public interface SuperTyped<T> {
    default T asSuper() {
        //noinspection unchecked
        return (T) this;
    }
}
