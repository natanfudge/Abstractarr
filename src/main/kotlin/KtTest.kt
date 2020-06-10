//import kotlin.random.Random
//
//class Orig : IOrig {
//    lateinit var arrField: Array<Orig?>
//
//    private val id = Random.nextInt(0, 20)
//    override fun toString(): String {
//        return id.toString()
//    }
//}
//
//
//interface IOrig {
//    fun setArrField(arrField: Array<IOrig?>) {
//        (this as Orig).arrField = arrField as Array<Orig?>
//    }
//
//    fun getArrField() : Array<IOrig?> = (this as Orig).arrField as Array<IOrig?>
//
//    companion object {
//        fun create() = Orig() as IOrig
//    }
//}
//
//fun main() {
//    apiSafe(IOrig.create())
//}
//
//fun apiSafe(obj: IOrig) {
//    val safeObj = IOrig.create()
//    val arr = safeArray(3)
//    arr[0] = safeObj
//    arr[1] = IOrig.create()
//    obj.setArrField(arr)
//    println(obj.getArrField().joinToString())
//}
//
//
//private fun safeArray(size : Int) : Array<IOrig?> {
//    val unsafeArr : Array<Orig?> = arrayOfNulls(size)
//    unsafeArr as Array<IOrig?>
//    unsafeArr[0] = IOrig.create()
//    return unsafeArr
//}