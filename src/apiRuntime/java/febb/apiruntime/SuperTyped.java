package febb.apiruntime;

import net.minecraft.TestInterface;

import java.util.ArrayList;

public interface SuperTyped<T> {
    default T asSuper() {
        //noinspection unchecked
        return (T) this;
    }
}
