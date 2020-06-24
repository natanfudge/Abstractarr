public class Arg {

    static class Foo extends Bar<String[]>{
        void bar(String t){

        }
    }
    static class Bar<T> {
        void bar(T t){

        }
    }
}
