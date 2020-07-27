package abstractor
import metautils.util.Tree


//
//import metautils.api.*
//import metautils.util.*
//
////data class AbstractionTargetGraph
//
////@PublishedApi
////internal fun getReferencedClasses(
////    allClasses: Collection<ClassApi>,
////    selected: TargetSelector
////): Set<QualifiedName> {
////    return allClasses.filter { selected.classes(it).isAbstracted }
////        .flatMap { it.getAllReferencedClasses(selected) }.toSet()
////}
//
//private data class Filter(
//    val classes: (ClassApi) -> Boolean,
//    val methods: (ClassApi.Method) -> Boolean,
//    val fields: (ClassApi.Field) -> Boolean
//)
//
//private val builtInCommonFilter = Filter(
//    classes = { it.isPublicApiAsOutermostMember }
//)
//
//
//
////private fun isBaseclass
//
//class AbstractionFilter(private val mcClasses: Collection<ClassApi>, private val userSelection: TargetSelector) {
//    private val mcClassesByName = mcClasses.map { it.name to it }.toMap()
//
//    private val builtInBaseclassFilter = Filter(
//        classes = { !it.isFinal },
//        methods = { it.allowedInBaseclass() }
//    )
//
////TODO: before the filtering, add all non-constructor parent methods and make them distinct
//
//    private fun ClassApi.Method.allowedInBaseclass(): Boolean {
//        if (!isPublicApiAsOutermostMember) return false
//        if (isConstructor) return true
//        if(!isAccessibleAsAbstractedApi()) return false
//    }
//
//    private fun ClassApi.Field.isAccessibleAsAbstractedApi() = type.getContainedNamesRecursively().all {
//        val classApi = mcClassesByName[it] ?: return@all true
//        classApi.isPublicApi /*&& classApi.name in abstractedClasses*/ //TODO: implement this filter, needs to know of what gets filtered first
//    }
//
//    private fun ClassApi.Method.isAccessibleAsAbstractedApi() = getContainedNamesRecursively().all {
//        val classApi = mcClassesByName[it] ?: return@all true
//        classApi.isPublicApi /*&& classApi.name in abstractedClasses*///TODO: implement this filter
//    }
//}
//
////interface Referencer {
////    val referencing: Collection<QualifiedName>
////}
//
////TODO: split into 2 phases:
//// - Build graph (in data)
//// - Make decisions based on flat representation of graph
//class AbstractionSelection
//
//data class Vertex(val referencing: GraphNode, val referenced: GraphNode)
//
////private fun Iterable<Reference>.toGraph(referencorsOrder: List<GraphNode>): Graph {
////    val vertices = map { (referencing, referenced) -> referencing.name to referenced.shortName.toDotQualifiedString() }
////    return Graph(vertices, nodes = referencorsOrder.map { it.name })
////}
//
//data class Graph(
//    val vertices: Collection<Vertex>,
//    // Order matters because it changes how the graph looks
//    val nodes: List<GraphNode>
//)
//
////private fun Iterable<GraphNode>.referencedBy(by : GraphNode) =
//
//private fun Tree.mcReferences(mcClasses: Set<QualifiedName>) = getContainedNamesRecursively().filter { it in mcClasses }
//private fun Tree.mcReferencesAsVertices(mcClasses: Set<QualifiedName>, owner: GraphNode) =
//    mcReferences(mcClasses).map { Vertex(owner, it) }
//
//private fun ClassApi.getReferences(mcClasses: Iterable<QualifiedName>): List<Vertex> {
//    val mcClassesSet = mcClasses.toSet()
//    val members = (fields + methods).toSet()
//    val otherChildren = getDirectChildren().filter { it !in members }
//    val displayedMembers = members.filter { it.mcReferences(mcClassesSet).isNotEmpty() }
//    return displayedMembers.map { Vertex(this, it) } +
//            otherChildren.flatMap { it.mcReferencesAsVertices(mcClassesSet, owner = this) } +
//            displayedMembers.flatMap { it.mcReferencesAsVertices(mcClassesSet, owner = it) }
//}
////    fields.map { Vertex(referencing = this, referenced = it) } +
////            methods.map { Vertex(referencing = this, referenced = it) } +
////            getDirectChildren().flatMap { it.references(owner = if (it is GraphNode) it else this) }
//
//
////fun ClassApi.testGraphString() =
////    Graph(getReferences(), this.prependTo(getDirectChildren().filterIsInstance<GraphNode>()))
////        .testPrintableString()
//
//private fun ClassApi.getNodes() = this.prependTo(getDirectChildren().filterIsInstance<GraphNode>())
//
//private fun GraphNode.height(firstRowClasses: Set<ClassApi>): Int {
//    var height = 0
//
//    // First row classes are the height
//    if (this in firstRowClasses) height += 1
//    else height -= 1
//
//    // Classes come before methods
//    if (this is ClassApi) height += 10
//    else height -= 10
//
//    return height
//}
//
//fun Iterable<ClassApi>.toGraphvizString(firstRowClasses: Collection<ClassApi>): String {
//    val firstRowClassesSet = firstRowClasses.fastToSet()
//    val vertices = flatMap { classApi -> classApi.getReferences(this.map { it.name }) }
//    return Graph(
//        vertices,
//        nodes = vertices.map { it.referencing }.sortedByDescending { it.height(firstRowClassesSet) }
//            .distinctBy { it.globallyUniqueIdentifier }
//    ).toGraphvizString()
//    //    val vertices = (firstRowClasses + this).distinct().flatMap { it.getReferences() }.distinct()
//}
//
//
interface GraphNode : Tree {
    val presentableName: String
    val globallyUniqueIdentifier: String
}
//
////private fun GraphNode.
//
//private fun Graph.toGraphvizString(): String {
//    val nodes =
//        nodes.joinToString("\n") { "${it.globallyUniqueIdentifier.withQuotes()}[label = ${it.presentableName.withQuotes()}];" } + "\n\n\n"
//
//    val vertexGroups = vertices.groupBy { it.referencing.globallyUniqueIdentifier }
//    val vertices = vertexGroups.map { (referencing, referenced) ->
//        val multiple = referenced.size > 1
//        "${referencing.withQuotes()} -> " + "{".includeIf(multiple) + referenced.joinToString(" ") {
//            it.referenced.globallyUniqueIdentifier.withQuotes()
//        } + "}".includeIf(multiple) + ";"
//    }.joinToString("\n")
//
//    return nodes + "\n\n\n\n" + vertices
//}
//
//private fun String.withQuotes() = "\"" + this + "\""
//
////            vertices.joinToString("\n") { "\"${it.referencing.uniqueIdentifier}\" -> \"${it.referenced.uniqueIdentifier}\";" }
//
////abstract class Named(override val name: String) : GraphNode
//
////private fun Tree.references(owner: GraphNode) = mapNames { Vertex(owner, it) }
////private fun ClassApi.Method.getReferences(): List<Reference> = mapNames { Reference(this, it) }
////private fun ClassApi.Field.getReferences(): List<Reference> = mapNames { Reference(this, it) }
//
//////@PublishedApi
//////internal fun getReferencedClasses(
//////    allClasses: Collection<ClassApi>,
//////    selected: TargetSelector
//////): Set<QualifiedName> {
//////    return allClasses.filter { selected.classes(it).isAbstracted }
//////        .flatMap { it.getAllReferencedClasses(selected) }.toSet()
//////}