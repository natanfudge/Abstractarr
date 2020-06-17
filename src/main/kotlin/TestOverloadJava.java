import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestOverloadJava {
    public static void test(List<String> foo) {
        int x = (int)2.0;
    }

//    public static void test(List<Boolean> foo) {
//        TestOverloadKt.foo(Collections.singletonList(false));
//        TestOverloadKt.foo(Collections.singletonList("false"));
//    }
}
