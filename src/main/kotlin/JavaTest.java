public class JavaTest {
    public static IOrig[] array(int size) {
        return new Orig[size];
    }
}

class Foo<T>{
    public static void bar() {
        Foo<Boolean> x = new Foo<Boolean>();
    }
}



//private fun safeArray(size : Int) : Array<IOrig?> {
//    val unsafeArr : Array<Orig?> = arrayOfNulls(size)
//    unsafeArr as Array<IOrig?>
//    unsafeArr[0] = IOrig.create()
//    return unsafeArr
//}