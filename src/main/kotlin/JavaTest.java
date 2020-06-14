import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;

//public class JavaTest extends ArrayList<?> {
//    public static IOrig[] array(int size) {
//        return new Orig[size];
//    }
//}

@Target(ElementType.TYPE_USE)
@interface  foo {

}

class Foo<T>{
    public static void bar()  throws @foo RuntimeException {
        Foo<Boolean> x = new Foo<Boolean>();
    }
}



//private fun safeArray(size : Int) : Array<IOrig?> {
//    val unsafeArr : Array<Orig?> = arrayOfNulls(size)
//    unsafeArr as Array<IOrig?>
//    unsafeArr[0] = IOrig.create()
//    return unsafeArr
//}