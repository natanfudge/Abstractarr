package codegeneration



enum class ClassVariant {
    Interface,
    ConcreteClass,
    AbstractClass,
    Enum,
    Annotation
}

data class ClassAccess(val isFinal: Boolean, val variant: ClassVariant)
data class MethodAccess(
    val isStatic: Boolean,
    val isFinal: Boolean,
    val isAbstract: Boolean,
    val visibility: Visibility
)

sealed class Visibility {
    companion object

    object Protected : Visibility() {
        override fun toString(): String = "protected "
    }
}

sealed class ClassVisibility : Visibility() {
    object Public : ClassVisibility() {
        override fun toString(): String = "public "
    }

    object Private : ClassVisibility() {
        override fun toString(): String = "private "
    }

    object Package : ClassVisibility() {
        override fun toString(): String = ""
    }
}

val Visibility.isPrivate get() = this == ClassVisibility.Private
