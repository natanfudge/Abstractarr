import net.minecraft.TestAbstractClass
import net.minecraft.TestConcreteClass
import net.minecraft.TestGenerics
import net.minecraft.TestOtherClass
import org.junit.jupiter.api.Test

//@Test
//fun testMcOnly() {
//    // This method can only be run here because of overload resolution ambiguity
//}

fun TestGenerics<ArrayList<TestConcreteClass>, ArrayList<TestConcreteClass>,
List<ArrayList<TestConcreteClass>>, Int>.testBaseclass(eParam : List<String>){
    genericMethod<ArrayList<TestConcreteClass>>(null,null,eParam,
        listOf<TestAbstractClass>(),null, null,null, TestOtherClass())
}

fun TestGenerics.Extendor<ArrayList<TestConcreteClass>>.testBaseclassExtendor(eParam : TestOtherClass){
    genericMethod<ArrayList<TestConcreteClass>>(null,null,null,
        listOf<TestAbstractClass>(),null, null,null, eParam)
}