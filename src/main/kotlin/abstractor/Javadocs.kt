package abstractor

import metautils.types.jvm.MethodDescriptor
import metautils.types.jvm.fromDescriptorString
import metautils.util.QualifiedName
import metautils.util.flatMapNotNull
import metautils.util.toQualifiedName
import net.fabricmc.mapping.tree.*
import java.nio.file.Files
import java.nio.file.Path

sealed class Documentable {
    data class Class(val name: QualifiedName) : Documentable()
    data class Method(val parentClass: Class, val name: String, val descriptor: MethodDescriptor) : Documentable()
    data class Field(val parentClass: Class, val name: String) : Documentable()
    data class Parameter(val parentMethod: Method, val index: Int) : Documentable()
}

private typealias Docs<T> = Map<T, String>

class JavaDocs(
    private val classes: Docs<Documentable.Class>,
    private val methods: Docs<Documentable.Method>,
    private val fields: Docs<Documentable.Field>,
    private val parameters: Docs<Documentable.Parameter>
) {
    fun getDoc(documentable: Documentable) : String? = when(documentable){
        is Documentable.Class -> getClassDoc(documentable)
        is Documentable.Method -> getMethodDoc(documentable)
        is Documentable.Field -> getFieldDoc(documentable)
        is Documentable.Parameter -> getParameterDoc(documentable)
    }
    fun getClassDoc(doc: Documentable.Class) = classes[doc]
    fun getMethodDoc(doc: Documentable.Method) = methods[doc]
    fun getFieldDoc(doc: Documentable.Field) = fields[doc]
    fun getParameterDoc(doc: Documentable.Parameter) = parameters[doc]
//    fun getClassDoc(className: QualifiedName): String? = classes[abstractor.Documentable.Class(className)]
//
//    fun getMethodDoc(className: QualifiedName, methodName: String, descriptor: MethodDescriptor): String? {
//        return methods[abstractor.Documentable.Method(abstractor.Documentable.Class(className), methodName, descriptor)]
//    }
//
//    fun getParameterDoc(
//        className: QualifiedName, methodName: String, descriptor: MethodDescriptor, index: Int
//    ): String? {
//        return parameters[abstractor.Documentable.Parameter(
//            abstractor.Documentable.Method(abstractor.Documentable.Class(className), methodName, descriptor), index
//        )]
//    }
//
//    fun getFieldDoc(className: QualifiedName, fieldName: String): String? {
//        return fields[abstractor.Documentable.Field(abstractor.Documentable.Class(className), fieldName)]
//    }

    companion object {
        val Empty = JavaDocs(mapOf(), mapOf(), mapOf(), mapOf())
        private fun Mapped.yarnName() = getName("named")
        private fun Descriptored.yarnDescriptor() = getDescriptor("named")
        private fun ClassDef.documentable(): Documentable.Class {
            return Documentable.Class(yarnName().toQualifiedName(dotQualified = false))
        }

        private fun MethodDef.documentable(parentClass: ClassDef): Documentable.Method {
            return Documentable.Method(
                parentClass.documentable(),
                yarnName(),
                MethodDescriptor.fromDescriptorString(yarnDescriptor())
            )
        }

        private fun ParameterDef.documentable(parentMethod: MethodDef, parentClass: ClassDef): Documentable.Parameter {
            return Documentable.Parameter(
                parentMethod.documentable(parentClass),
                localVariableIndex
            )
        }

        private fun FieldDef.documentable(parentClass: ClassDef): Documentable.Field {
            return Documentable.Field(parentClass.documentable(), yarnName())
        }

        private fun <T : Documentable> Mapped.docWith(documentable: T) = comment?.let { documentable to it }

        fun readTiny(tinyFile: Path): JavaDocs {
            val mappings = Files.newBufferedReader(tinyFile).use { reader ->
                TinyMappingFactory.load(reader)
            }

            return JavaDocs(
                classes = mappings.classes.mapNotNull { it.docWith(it.documentable()) }.toMap(),
                methods = mappings.classes.flatMapNotNull { parentClass ->
                    parentClass.methods.map { it.docWith(it.documentable(parentClass)) }
                }.toMap(),
                parameters = mappings.classes.flatMap { parentClass ->
                    parentClass.methods.flatMapNotNull { parentMethod ->
                        parentMethod.parameters.map { it.docWith(it.documentable(parentMethod, parentClass)) }
                    }
                }.toMap(),
                fields = mappings.classes.flatMapNotNull { parentClass ->
                    parentClass.fields.map { it.docWith(it.documentable(parentClass)) }
                }.toMap()
            )

        }
    }
}

