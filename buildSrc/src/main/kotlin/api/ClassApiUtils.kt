package api

val ClassApi.isFinal get() = access.isFinal


/**
 * Goes from top to bottom
 */
@OptIn(ExperimentalStdlibApi::class)
fun ClassApi.listInnerClassChain(): List<ClassApi> = buildList<ClassApi> { addToInnerClassChain(this) }.reversed()
private fun ClassApi.addToInnerClassChain(accumulated: MutableList<ClassApi>) {
    accumulated.add(this)
    outerClass?.value?.addToInnerClassChain(accumulated)
}

fun ClassApi.visitClasses(visitor: (ClassApi) -> Unit) {
    visitor(this)
    innerClasses.forEach(visitor)
}