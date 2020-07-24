package abstractor

import metautils.api.ClassApi
import metautils.util.*

//data class AbstractionTargetGraph

//@PublishedApi
//internal fun getReferencedClasses(
//    allClasses: Collection<ClassApi>,
//    selected: TargetSelector
//): Set<QualifiedName> {
//    return allClasses.filter { selected.classes(it).isAbstracted }
//        .flatMap { it.getAllReferencedClasses(selected) }.toSet()
//}
class AbstractionFilter(private val mcClasses: Collection<ClassApi>, private val userSelection: TargetSelector) {
    private val mcClassesByName = mcClasses.map { it.name to it }.toMap()
//    fun filter(): Collection<ClassApi> {
////        val referencedClasses = getReferencedClasses(classNamesToClasses.values, metadata.selector)
////                // Also add the outer classes, to prevent cases where only an inner class is abstracted and not the outer one.
////                val allReferencedClasses = referencedClasses.flatMap { it.thisToOuterClasses() }.toSet()
////                val abstractedClasses = classNamesToClasses.values.filter {                // Minimal classes are classes not chosen to be abstracted, but are
////                    // This also adds "minimal" classes:
////                    // Minimal classes are classes not chosen to be abstracted, but are referenced by classes that are.
////                    // Those classes are abstracted, but only contain methods that have abstracted classes in their
////                    // signature. This is done to maximize the amount of exposed methods while minimizing the amount
////                    // of exposed classes.
////                    it.isPublicApi && (metadata.selector.classes(it).isAbstracted || it.name in allReferencedClasses)
////                }
//    }

    //    private fun methodsAddedInBaseclass(allMethods: List<ClassApi.Method>): BaseclassMethodsAddedInfo {
//        val distinctMethods = allMethods.distinctBy { it.uniqueIdentifier() }
//        val relevantMethods = distinctMethods.filter {
//            // Constructors are handled separately
//            !it.isConstructor && it.isAccessibleAsAbstractedApi()
//                    && metadata.selector.methods(classApi, it).addInBaseclass
//        }
//
//        val bridgeMethods = relevantMethods.filter { method ->
//            val containsMc = method.descriptorContainsMcClasses()
//            if (!method.isPublic && !method.isProtected) return@filter false
//
//            // The purpose of bridge methods is to get calls from mc to call the methods from the api, but when
//            // there is no mc classes involved the methods are the same as the mc ones, so when mc calls the method
//            // it will be called in the api as well (without needing a bridge method)
//            return@filter containsMc
//        }
//
//        // Baseclasses don't inherit the baseclasses of their superclasses, so we need to also add all the methods
//        // of the superclasses
//        val apiMethods = distinctMethods.filter { method ->
//            // Constructors are handled separately
//            if (method.isConstructor || !method.isAccessibleAsAbstractedApi()
//                || !metadata.selector.methods(classApi, method).addInBaseclass
//            ) return@filter false
//            val containsMc = method.descriptorContainsMcClasses()
//
//            return@filter if (method.isProtected || (method.isPublic && classApi.isProtected)) {
//                // Multiple things to consider here.
//                // 1. In the implementation jar (fitToPublicApi = false), there's no need to add methods that don't contain
//                // mc classes, because when an api user calls a method without mc classes, the jvm will just look up
//                // to the mc class and call it. But when a mc class is in the descriptor, the api method descriptor
//                // will get remapped to have api classes, so the call will no longer be valid for the originally declared
//                // mc methods.
//                // We also actively cannot add a "containsMc = false" method when mc declares it as final,
//                // because it will be considered as an override for the mc declared method. (Which is not allowed for final methods)
//                // 2. In the api jar (fitToPublicApi = true), the mc methods are not seen by the user so we need to also add
//                // "containsMc = false" methods. There is no problem of overriding because it's not passed through a JVM verifier.
//                if (!metadata.fitToPublicApi) containsMc
//                else true
//            } else if (method.isPublic) {
//                // We need to add our own override to the method because we want the bridge method
//                // to call the mc method (with a super call) by default.
//                // If we don't add this method here to override the api method, it will call the method in the api interface,
//                // which will call the bridge method - infinite recursion.
//                // This override is not needed to be seen by the user.
//
//                // We want to avoid users creating lambdas of api interfaces because they are not meant to be implemented, and make them make lambdas of the baseclasses instead.
//                // So when the class is a SAM Interface, we make the api interface not have a single abstract method, and we make the baseclass have a single abstract method.
//                if (metadata.fitToPublicApi) classApi.isSamInterface() && method.isAbstract
//                else !method.isStatic && containsMc
//            } else false
//        }
//        return BaseclassMethodsAddedInfo(apiMethods, bridgeMethods)
//    }
//    private fun ClassApi.Method.isBaseclassAbstracted(): Boolean {
//        if (isConstructor) {
//            return isPublic || isProtected
//        }
//    }
}

//interface Referencer {
//    val referencing: Collection<QualifiedName>
//}

//TODO: split into 2 phases:
// - Build graph (in data)
// - Make decisions based on flat representation of graph
class AbstractionSelection

data class Vertex(val referencing: GraphNode, val referenced: GraphNode)

//private fun Iterable<Reference>.toGraph(referencorsOrder: List<GraphNode>): Graph {
//    val vertices = map { (referencing, referenced) -> referencing.name to referenced.shortName.toDotQualifiedString() }
//    return Graph(vertices, nodes = referencorsOrder.map { it.name })
//}

data class Graph(
    val vertices: Collection<Vertex>,
    // Order matters because it changes how the graph looks
    val nodes: List<GraphNode>
)

//private fun Iterable<GraphNode>.referencedBy(by : GraphNode) =

private fun Tree.mcReferences(mcClasses: Set<QualifiedName>) = getContainedNamesRecursively().filter { it in mcClasses }
private fun Tree.mcReferencesAsVertices(mcClasses: Set<QualifiedName>, owner: GraphNode) =
    mcReferences(mcClasses).map { Vertex(owner, it) }

private fun ClassApi.getReferences(mcClasses: Iterable<QualifiedName>): List<Vertex> {
    val mcClassesSet = mcClasses.toSet()
    val members = (fields + methods).toSet()
    val otherChildren = getDirectChildren().filter { it !in members }
    val displayedMembers = members.filter { it.mcReferences(mcClassesSet).isNotEmpty() }
    return displayedMembers.map { Vertex(this, it) } +
            otherChildren.flatMap { it.mcReferencesAsVertices(mcClassesSet, owner = this) } +
            displayedMembers.flatMap { it.mcReferencesAsVertices(mcClassesSet, owner = it) }
}
//    fields.map { Vertex(referencing = this, referenced = it) } +
//            methods.map { Vertex(referencing = this, referenced = it) } +
//            getDirectChildren().flatMap { it.references(owner = if (it is GraphNode) it else this) }


//fun ClassApi.testGraphString() =
//    Graph(getReferences(), this.prependTo(getDirectChildren().filterIsInstance<GraphNode>()))
//        .testPrintableString()

private fun ClassApi.getNodes() = this.prependTo(getDirectChildren().filterIsInstance<GraphNode>())

private fun GraphNode.height(firstRowClasses: Set<ClassApi>): Int {
    var height = 0

    // First row classes are the height
    if (this in firstRowClasses) height += 1
    else height -= 1

    // Classes come before methods
    if (this is ClassApi) height += 10
    else height -= 10

    return height
}

fun Iterable<ClassApi>.toGraphvizString(firstRowClasses: Collection<ClassApi>): String {
    val firstRowClassesSet = firstRowClasses.fastToSet()
    val vertices = flatMap { classApi -> classApi.getReferences(this.map { it.name }) }
    return Graph(
        vertices,
        nodes = vertices.map { it.referencing }.sortedByDescending { it.height(firstRowClassesSet) }
            .distinctBy { it.globallyUniqueIdentifier }
    ).toGraphvizString()
    //    val vertices = (firstRowClasses + this).distinct().flatMap { it.getReferences() }.distinct()
}


interface GraphNode : Tree {
    val presentableName: String
    val globallyUniqueIdentifier: String
}

//private fun GraphNode.

private fun Graph.toGraphvizString(): String {
    val nodes = nodes.joinToString("\n") { "${it.globallyUniqueIdentifier.withQuotes()}[label = ${it.presentableName.withQuotes()}];" } + "\n\n\n"

    val vertexGroups = vertices.groupBy { it.referencing.globallyUniqueIdentifier }
    val vertices = vertexGroups.map { (referencing, referenced) ->
        val multiple = referenced.size > 1
        "${referencing.withQuotes()} -> " + "{".includeIf(multiple) + referenced.joinToString(" ") {
            it.referenced.globallyUniqueIdentifier.withQuotes()
        } + "}".includeIf(multiple) + ";"
    }.joinToString("\n")

    return nodes + "\n\n\n\n" + vertices
}

private fun String.withQuotes() = "\"" + this + "\""

//            vertices.joinToString("\n") { "\"${it.referencing.uniqueIdentifier}\" -> \"${it.referenced.uniqueIdentifier}\";" }

//abstract class Named(override val name: String) : GraphNode

//private fun Tree.references(owner: GraphNode) = mapNames { Vertex(owner, it) }
//private fun ClassApi.Method.getReferences(): List<Reference> = mapNames { Reference(this, it) }
//private fun ClassApi.Field.getReferences(): List<Reference> = mapNames { Reference(this, it) }

////@PublishedApi
////internal fun getReferencedClasses(
////    allClasses: Collection<ClassApi>,
////    selected: TargetSelector
////): Set<QualifiedName> {
////    return allClasses.filter { selected.classes(it).isAbstracted }
////        .flatMap { it.getAllReferencedClasses(selected) }.toSet()
////}