import abstractor.VersionPackage
import abstractor.isMcClass
import abstractor.isMcClassName
import api.*
import codegeneration.*
import descriptor.JvmPrimitiveType
import descriptor.JvmType
import descriptor.ObjectType
import descriptor.ReturnDescriptor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.InnerClassNode
import signature.*
import util.*
import java.nio.file.Path


data class AbstractionMetadata(
    val versionPackage: VersionPackage,
    val classPath: List<Path>,
    val fitToPublicApi: Boolean,
    val writeRawAsm: Boolean
)

//TODO: let the asm thing accept a classpath index, and add access to the classpath index, instead of the hardcoding we have now.


//TODO: for parameter names and docs, generate a java source alongside the asm source.


// TODO: handle protected methods, they should be exposed through a seperate api class that is only applied to baseclasses

//TODO:
// baseclasses
//TODO: list of wanted asm magic:
// - jdk interfaces of mc classes
// - Overloads with different return type (i.e. methods that the only mc class in it is in the return type)
// - Remove casts
// - Final interfaces
// - Add type bounds without them being checked (enums, other supertyped cases)
// - Maybe all generics in general should be with asm?
// i.e.:
//class McClass1<T extends JdkClass>
//class MCClass2 extends JdkClass
//class McClass3 extends McClass1<McClass2>
//
//-- >
//interface IMcClass1<T cextends JdkClass>
//interface IMCClass2 extends SuperTyped<JdkClass>
//interface IMcClass3 extends SuperTyped<IMcClass2> // boom!!! IMcClass2 does not extend JdkClass

object Abstractor {
    fun abstract(mcJar: Path, destDir: Path, metadata: AbstractionMetadata) {
        require(destDir.parent.exists()) { "The chosen destination path '$destDir' is not in any existing directory." }
        require(destDir.parent.isDirectory()) { "The parent of the chosen destination path '$destDir' is not a directory." }

        destDir.deleteRecursively()
        destDir.createDirectory()

        val index = indexClasspath(metadata.classPath + listOf(mcJar) /*+ getJvmBootClasses()*/)

        val classes = ClassApi.readFromJar(mcJar)

        for (classApi in classes
//            .filter { it.name.shortName.startsWith("ExtendedInterface") }
        ) {
            if (!classApi.isPublicApi) continue
            ClassAbstractor(metadata, index, classApi/*, baseClass = false*/).abstractClass(
                destPath = destDir,
                outerClass = null
            )
        }
    }
}


private data class ClassAbstractor(
    private val metadata: AbstractionMetadata,
    private val index: ClasspathIndex,
    private val classApi: ClassApi
) {

    val apiInterfaceAccess = ClassAccess(
        isFinal = metadata.fitToPublicApi,
        variant = ClassVariant.Interface
    )
    val apiClassVisibility = ClassVisibility.Public

    fun baseClassAccess(origIsInterface: Boolean) = ClassAccess(
        isFinal = false,
        variant = if (origIsInterface) ClassVariant.Interface else ClassVariant.AbstractClass
    )


    fun abstractClass(outerClass: GeneratedClass?, destPath: Path?) {
        check(classApi.visibility == ClassVisibility.Public)
        createApiInterface(outerClass, destPath)
        if (!classApi.isFinal) createBaseclass(outerClass, destPath)
    }
    // No need to add I for inner classes

    private fun apiClassName(outerClass: GeneratedClass?, nameMap: (QualifiedName) -> QualifiedName) =
        if (outerClass == null) nameMap(classApi.name) else classApi.name

    fun createBaseclass(outerClass: GeneratedClass?, destPath: Path?) = with(metadata.versionPackage) {
        val (packageName, shortName) = apiClassName(outerClass) { it.toBaseClass() }

        val mcClass = classApi.asType()

        val interfaces = listOf(
            mcClass.remapToApiClass().pushAllTypeArgumentsToInnermostClass()
        ).applyIf(classApi.isInterface) { it + mcClass } + classApi.superInterfaces.remapToApiClasses()


        val classInfo = ClassInfo(
            visibility = apiClassVisibility,
            access = baseClassAccess(classApi.isInterface),
            shortName = shortName.innermostClass(),
            typeArguments = baseClassTypeArguments(),
            superInterfaces = interfaces,
            superClass = if (!classApi.isInterface && !metadata.fitToPublicApi) {
                classApi.asRawType()/*.applyIf(metadata.fitToPublicApi) { it.remapToBaseClass() }*/
            } else null,
            annotations = /*classApi.annotations*/ listOf() // Translating annotations can cause compilation errors...
        ) { addBaseclassBody() }

        writeClass(destPath, classInfo, packageName, outerClass, isInnerClassStatic = classApi.isStatic)
    }

    /**
     * destPath == null and outerClass != null when it's an inner class
     */
    fun createApiInterface(outerClass: GeneratedClass?, destPath: Path?) = with(metadata.versionPackage) {
        // No need to add I for inner classes
        val (packageName, shortName) = apiClassName(outerClass) { it.toApiClass() }


        // Add SuperTyped<SuperClass> superinterface for classes which have a non-mc superclass
        val superTypedInterface = getSuperTypedInterface()
        //TODO: in the distributed version, add in the jdk interfaces
        val interfaces = classApi.superInterfaces
            .remapToApiClasses().appendIfNotNull(superTypedInterface)


        val classInfo = ClassInfo(
            visibility = apiClassVisibility,
            access = apiInterfaceAccess,
            shortName = shortName.innermostClass(),
            typeArguments = allApiInterfaceTypeArguments(),
            superInterfaces = interfaces,
            superClass = null,
            annotations = /*classApi.annotations*/ listOf() // Translating annotations can cause compilation errors...
        ) { addApiInterfaceBody() }

        writeClass(destPath, classInfo, packageName, outerClass, isInnerClassStatic = true)
    }

    private fun writeClass(
        destPath: Path?,
        classInfo: ClassInfo,
        packageName: PackageName?,
        outerClass: GeneratedClass?,
        isInnerClassStatic: Boolean
    ) {
        val codegen = if (metadata.writeRawAsm) AsmCodeGenerator else JavaCodeGenerator
        if (destPath != null) {
            codegen.writeClass(classInfo, packageName, destPath)
        } else {
            requireNotNull(outerClass)
            outerClass.addInnerClass(classInfo, isStatic = isInnerClassStatic)
        }
    }

    private fun getSuperTypedInterface() = classApi.superClass?.let {
        when {
            it.isMcClass() -> it.remapToApiClass()
            it.satisfyingTypeBoundsIsImpossible() -> null
            else -> ClassGenericType(
                SuperTypedPackage,
                SimpleClassGenericType(
                    SuperTypedName,
                    TypeArgument.SpecificType(
                        /*if (it.containsMcClasses()) it.remapToApiClass().type else it.type*/ it.remapToApiClass().type,
                        wildcardType = null
                    ).singletonList()
                ).singletonList()
            ).noAnnotations()
        }

    }


    // TODO: This will do for now. A proper solution will check if:
    // 1. This is used as a superclass in this context (this is already the case when this method is called)
    // 2. This is a non-mc class
    // 3. Any type arguments are bound by a type that contains themselves (e.g. T extends Enum<T>)
    // ALTERNATIVELY------- instead of generating a SuperTyped<> interfaces just add the cast method itself to the class
    // OR do some bytecode shenanigans if that works
    private fun JavaClassType.satisfyingTypeBoundsIsImpossible() = type.packageName == EnumPackage
            && type.classNameSegments[0].name == EnumName


    private fun GeneratedClass.addBaseclassBody() {
        passAsmInnerClasses(baseClass = true)
        for (method in classApi.methods) {
            if (method.isConstructor && (method.isPublic || method.isProtected)) addBaseclassConstructor(method)
        }

        for (innerClass in classApi.innerClasses) {
            if (!innerClass.isPublic) continue
            if (!innerClass.isFinal) copy(classApi = innerClass).createBaseclass(destPath = null, outerClass = this)
        }
    }

    private fun GeneratedClass.addApiInterfaceBody() {
        passAsmInnerClasses(baseClass = false)
        for (method in classApi.methods) {
            if (method.isPublic) {
                if (method.isConstructor) addApiInterfaceFactory(method)
                else {
                    // Don't duplicate methods that are just being overriden
                    if (!method.isOverrideIgnoreReturnType(index, classApi)) {
                        addApiInterfaceDeclaredMethod(method)
                        addBridgeMethod(method)
                    }
                }
            }
        }

        for (field in classApi.fields) {
            abstractField(field)
        }
        for (innerClass in classApi.innerClasses) {
            if (!innerClass.isPublic) continue
            copy(classApi = innerClass).createApiInterface(destPath = null, outerClass = this)

            // Inner classes are constructed by their parent
            if (!innerClass.isStatic) {
                addInnerClassConstructor(innerClass)
            }
        }

        addArrayFactory()
    }

    private fun GeneratedClass.passAsmInnerClasses(baseClass: Boolean) {
        // Resolving the inner classes on our own is quite difficult, so we will just pass what we get from parsing the original mc class
        if (this is AsmGeneratedClass) {
            with(metadata.versionPackage) {
                val mcClasses =
                    classApi.asmInnerClasses.map { it.access to it.name.toQualifiedName(dotQualified = false) }
                        .filter { (_, name) -> name.isMcClassName() }
                val apiClasses = mcClasses
                    .map { (_, name) ->
                        val qualifiedName = name.toApiClass()
                        innerClassNode(qualifiedName, access = apiInterfaceAccess)
                    }

                val baseClasses = if (baseClass) {
                    mcClasses.map { (access, name) ->
                        val qualifiedName = name.toBaseClass()
                        innerClassNode(
                            qualifiedName,
                            access = baseClassAccess(origIsInterface = access and Opcodes.ACC_INTERFACE != 0)
                        )
                    }
                } else listOf()

                addAsmInnerClasses(apiClasses + baseClasses + classApi.asmInnerClasses)
            }
        }
    }

    private fun innerClassNode(qualifiedName: QualifiedName, access: ClassAccess): InnerClassNode {
        return InnerClassNode(
            qualifiedName.toSlashQualifiedString(),
            qualifiedName.copy(shortName = qualifiedName.shortName.outerClass())
                .toSlashQualifiedString(),
            qualifiedName.shortName.innermostClass(),
            access.toAsmAccess(apiClassVisibility, isStatic = true)
        )
    }
    //TODO: pull out the baseclass boolean and make it a whole new thing I think

    private fun GeneratedClass.addBridgeMethod(method: ClassApi.Method) {
        // Bridges only exist for you to override methods
        if (method.isFinal || method.isStatic) return
        // Bridge methods are implementation details
        if (metadata.fitToPublicApi) return
        // The purpose of bridge methods is to get calls from mc to call the methods from the api, but when
        // there is no mc classes involved the methods are the same as the mc ones, so when mc calls the method
        // it will be called in the api as well (without needing a bridge method)
        if (!method.returnType.isMcClass() && method.parameters.values.none { it.isMcClass() }) return

        val mcReturnType = method.returnType
        val mcClassType = classApi.asRawType()

        val methodInfo = method.apiMethodInfo(remapParameters = false) {
            val call = MethodCall.Method(
                receiver = ThisExpression,
                parameters = method.parameters.map { (name, type) ->
                    type.remapToApiClass().toJvmType() to VariableExpression(name)
                },
                name = method.name,
                methodAccess = method.access,
                receiverAccess = classApi.access.copy(variant = ClassVariant.Interface),
                returnType = mcReturnType.toJvmType(),
                // In a bridge method, call the api interface
                // (so when mc calls it it will reach the api overrides)
                owner = mcClassType.remapToApiClass().toJvmType()
            )

            if (!method.isVoid) {
                @Suppress("UNCHECKED_CAST")
                val returnedType = mcReturnType as AnyJavaType
                val returnedValue =call.castFromApiToMc(returnedType)
                addStatement(ReturnStatement(returnedValue))
            } else addStatement(call)
        }

        addMethod(
            methodInfo,
            name = method.name,
            isFinal = false,
            isStatic = method.isStatic,
            isAbstract = false,
            returnType = mcReturnType,
            typeArguments = method.typeArguments.remapDeclToApiClasses()
        )
    }


    private fun GeneratedClass.addApiInterfaceDeclaredMethod(method: ClassApi.Method) {

        val mcReturnType = method.returnType
        val returnType = mcReturnType.remapToApiClass()

        val mcClassType = classApi.asRawType()

        val methodInfo = method.apiMethodInfo(remapParameters = true) {
            val call = MethodCall.Method(
                receiver = if (method.isStatic) ClassReceiver(classApi.asJvmType())
                else ThisExpression.castFromApiToMc(mcClassType),
                parameters = apiPassedParameters(method),
                name = method.name,
                methodAccess = method.access,
                receiverAccess = classApi.access.copy(variant = classApi.access.variant),
                returnType = mcReturnType.toJvmType(),
                // In an api interface, call the mc method, in a baseclass, call the api interface
                // (so when mc calls it it will reach the api overrides)
                owner = mcClassType.toJvmType()
            )

            if (!method.isVoid) {
                @Suppress("UNCHECKED_CAST")
                val returnedValue = call.castFromMcToApi(mcReturnType as AnyJavaType)
                addStatement(ReturnStatement(returnedValue))
            } else addStatement(call)

        }

        addMethod(
            methodInfo,
            name = method.name,
            isFinal = false,
            isStatic = method.isStatic,
            isAbstract = false,
            returnType = returnType,
            typeArguments = method.typeArguments.remapDeclToApiClasses()
        )

    }

//    private fun apiMethodReturnType(returnType: JavaReturnType, baseClass: Boolean) =
//        returnType.applyIf(baseClass) { it.copy(annotations = it.annotations + OverrideAnnotation) }

    private fun GeneratedClass.addBaseclassConstructor(method: ClassApi.Method) {
        // Abstract classes/interfaces can't be constructed directly
        if (classApi.isInterface) return

        val mcClassType = classApi.asRawType()

        val methodInfo = method.apiMethodInfo(remapParameters = true) {
            val passedParameters = apiPassedParameters(method)
            addStatement(ConstructorCall.Super(passedParameters, superType = mcClassType.toJvmType()))
        }

        addConstructor(methodInfo)
    }

    private fun GeneratedClass.addApiInterfaceFactory(method: ClassApi.Method) {
        // Abstract classes/interfaces can't be constructed directly
        if (classApi.isInterface || classApi.isAbstract) return
        // Inner classes need to be constructed by their parent class in api interfaces
        if (classApi.isInnerClass && !classApi.isStatic) return


        val type = classApi.asType().pushAllTypeArgumentsToInnermostClass()
        val mcReturnType = type.copy(annotations = listOf(NotNullAnnotation))
        val returnType = mcReturnType.remapToApiClass()

        val mcClassType = classApi.asRawType()

        val methodInfo = method.apiMethodInfo(remapParameters = true) {
            val returnedValue = MethodCall.Constructor(
                constructing = mcClassType,
                parameters = apiPassedParameters(method),
                receiver = null
            ).castFromMcToApi(mcReturnType)
            addStatement(ReturnStatement(returnedValue))
        }

        addMethod(
            methodInfo,
            name = "create",
            isFinal = false,
            isStatic = true,
            isAbstract = false,
            returnType = returnType,
            typeArguments = allApiInterfaceTypeArguments()
        )
    }

    private fun ClassApi.Method.apiMethodInfo(
        remapParameters: Boolean,
        body: GeneratedMethod.() -> Unit
    ) = MethodInfo(
        visibility = visibility,
        throws = throws.map { it.remapToApiClass() },
        parameters = if (remapParameters) parameters else apiParametersDeclaration(this),
        body = body
    )

    private fun GeneratedClass.abstractField(field: ClassApi.Field) {
        if (!field.isPublic) return
        if (field.isFinal && field.isStatic) {
            addConstant(field)
        } else {
            addGetter(field)
        }

        if (!field.isFinal) {
            addSetter(field)
        }
    }

    private fun GeneratedClass.addConstant(field: ClassApi.Field) {
        addField(
            name = field.name, isFinal = true, isStatic = true, visibility = Visibility.Public,
            type = field.type.remapToApiClass(),
            initializer = abstractedFieldExpression(field).castFromMcToApi(field.type)
        )
    }

    private fun GeneratedClass.addGetter(field: ClassApi.Field) {
        val getterName = field.getGetterPrefix() +
                // When it starts with "is" no prefix is added so there's no need to capitalize
                if (field.name.startsWith(BooleanGetterPrefix)) field.name else field.name.capitalize()
        addMethod(
            // Add _field when getter clashes with a method of the same name
            name = if (classApi.methods.any { it.parameters.isEmpty() && it.name == getterName }) getterName + "_field"
            else getterName,
            parameters = mapOf(),
            visibility = Visibility.Public,
            returnType = field.type.remapToApiClass(),
            abstract = false,
            static = field.isStatic,
            final = metadata.fitToPublicApi,
            typeArguments = listOf(),
            throws = listOf()
        ) {
            val fieldAccess = abstractedFieldExpression(field)
            addStatement(
                ReturnStatement(
                    fieldAccess.castFromMcToApi(field.type)
                )
            )
        }
    }

    private fun ClassApi.Field.getGetterPrefix(): String =
        if (type.type.let { it is GenericsPrimitiveType && it.primitive == JvmPrimitiveType.Boolean }) {
            if (name.startsWith(BooleanGetterPrefix)) "" else BooleanGetterPrefix
        } else "get"

    private fun GeneratedClass.addSetter(field: ClassApi.Field) {
        addMethod(
            name = "set" + field.name.capitalize(),
            parameters = mapOf(field.name to field.type.remapToApiClass()),
            visibility = Visibility.Public,
            returnType = GenericReturnType.Void.noAnnotations(),
            abstract = false,
            static = field.isStatic,
            final = metadata.fitToPublicApi,
            typeArguments = listOf(),
            throws = listOf()
        ) {
            addStatement(
                AssignmentStatement(
                    target = abstractedFieldExpression(field),
                    assignedValue = VariableExpression(field.name).castFromApiToMc(field.type)
                )
            )
        }
    }

    private fun abstractedFieldExpression(
        field: ClassApi.Field
    ): FieldExpression {
        val mcClassType = classApi.asRawType()
        return FieldExpression(
            receiver = if (field.isStatic) ClassReceiver(classApi.asJvmType()) else ThisExpression.castFromApiToMc(
                mcClassType
            ),
            name = field.name,
            owner = classApi.asJvmType(),
            type = field.type.toJvmType()
        )
    }

    private fun GeneratedClass.addInnerClassConstructor(innerClass: ClassApi) {
        for (constructor in innerClass.methods.filter { it.isConstructor }) {
            val constructedInnerClass = innerClass.asType()
            addMethod(
                name = "new" + innerClass.name.shortName.innermostClass(),
                visibility = Visibility.Public,
                static = false,
                final = false,
                abstract = false,
                returnType = constructedInnerClass.remapToApiClass().pushAllTypeArgumentsToInnermostClass()
                    .copy(annotations = listOf(NotNullAnnotation)),
                parameters = apiParametersDeclaration(constructor),
                typeArguments = innerClass.typeArguments.remapDeclToApiClasses(),
                throws = constructor.throws
            ) {
                addStatement(
                    ReturnStatement(
                        MethodCall.Constructor(
                            constructing = constructedInnerClass,
                            parameters = apiPassedParameters(constructor),
                            receiver = ThisExpression.castFromApiToMc(classApi.asRawType())
                        ).castFromMcToApi(constructedInnerClass)
                    )
                )
            }
        }
    }

    private fun apiPassedParameters(method: ClassApi.Method): List<Pair<JvmType, Expression>> =
        method.parameters.map { (name, type) ->
            type.toJvmType() to VariableExpression(name).castFromApiToMc(type)
        }


    private fun apiParametersDeclaration(method: ClassApi.Method) =
        method.parameters.mapValues { (_, v) -> v.remapToApiClass() }.toMap()

    private fun GeneratedClass.addArrayFactory() {
        addMethod(
            name = if (existsMethodWithSameDescriptorAsArrayFactory()) "${ArrayFactoryName}_factory" else ArrayFactoryName,
            visibility = Visibility.Public,
            parameters = mapOf(ArrayFactorySizeParamName to GenericsPrimitiveType.Int.noAnnotations()),
            returnType = ArrayGenericType(classApi.asType().type).annotated(NotNullAnnotation).remapToApiClass()
                .pushAllArrayTypeArgumentsToInnermostClass(),
            abstract = false,
            final = false,
            static = true,
            typeArguments = allApiInterfaceTypeArguments(),
            throws = listOf()
        ) {
            addStatement(
                ReturnStatement(
                    ArrayConstructor(
                        classApi.asRawType(),
                        size = VariableExpression(ArrayFactorySizeParamName)
                    )
                )
            )
        }
    }

    private fun existsMethodWithSameDescriptorAsArrayFactory() = classApi.methods.any { method ->
        method.name == ArrayFactoryName && method.parameters.size == 1
                && method.parameters.values.first().type.let { it == GenericsPrimitiveType.Int }
    }

    internal fun allApiInterfaceTypeArguments(): List<TypeArgumentDeclaration> = when {
        classApi.isStatic -> classApi.typeArguments.remapDeclToApiClasses()
        else -> classApi.listInnerClassChain().flatMap { it.typeArguments.remapDeclToApiClasses() }
    }

    private fun baseClassTypeArguments(): List<TypeArgumentDeclaration> {
        return classApi.typeArguments.map { typeArg ->
            // For baseclasses, if a type is bounded by a mc type, we want to bound it in the api by both that mc type and the api version of it
            val classBound =
                if (typeArg.classBound?.isMcClass() == true) typeArg.classBound else typeArg.classBound?.remapToApiClass()
            val mcInterfaceBounds = typeArg.interfaceBounds.filter { it.isMcClass() }
            typeArg.copy(
                classBound = classBound,
                interfaceBounds = mcInterfaceBounds + typeArg.interfaceBounds.map { it.remapToApiClass() }
                    .applyIf(typeArg.classBound?.isMcClass() == true) { it + typeArg.classBound!!.remapToApiClass() }
//                    .appendIfNotNull()
            )
        }
    }

//    private fun GenericType.andRemappedToMcClass() = listOf(this, this.remapToApiClass())


////////// REMAPPING /////////

    private fun <T : GenericReturnType> JavaType<T>.remapToApiClass(): JavaType<T> =
        with(metadata.versionPackage) { remapToApiClass() }

    private fun <T : GenericReturnType> T.remapToApiClass(): T =
        with(metadata.versionPackage) { remapToApiClass() }

    private fun <T : ReturnDescriptor> T.remapToApiClass(): T =
        with(metadata.versionPackage) { remapToApiClass() }

    private fun List<TypeArgumentDeclaration>.remapDeclToApiClasses(): List<TypeArgumentDeclaration> =
        with(metadata.versionPackage) { remapDeclToApiClasses() }

/////////// CASTING //////////

    /**
     * For any type Child, and type Super, such that Super is a supertype of Child:
     * Casting from Child[] to Super[] is valid,
     * Casting from Super[] to Child[] will always fail.
     */

    private fun Expression.castFromMcToApi(mcClass: AnyJavaType) = cast(mcClass, mcClass.remapToApiClass())
    private fun Expression.castFromApiToMc(mcClass: AnyJavaType) = cast(mcClass.remapToApiClass(), mcClass)

    private fun Expression.cast(fromType: AnyJavaType, toType: AnyJavaType) = if (fromType.isAssignableTo(toType)) this
    else CastExpression(this, toType)

    private fun JvmType.isAssignableTo(otherType: JvmType): Boolean {
        if (this == otherType) return true
        if (this.remapToApiClass() == otherType) return true
        // Technically this can be true in more cases but that requires using the classpath and it's not relevant for our purposes
        return false
    }

    // When assigning types, only the underlying JvmType matters
    private fun AnyJavaType.isAssignableTo(otherType: AnyJavaType) = toJvmType().isAssignableTo(otherType.toJvmType())
}


private val SuperTypedPackage = PackageName(listOf("febb", "apiruntime"))
private const val SuperTypedName = "SuperTyped"
private val EnumPackage = PackageName(listOf("java", "lang"))
private const val EnumName = "Enum"

private const val BooleanGetterPrefix = "is"
private const val ArrayFactoryName = "array"
private const val ArrayFactorySizeParamName = "size"
private val NotNullAnnotation = JavaAnnotation(ObjectType("org/jetbrains/annotations/NotNull", dotQualified = false))
private val OverrideAnnotation = JavaAnnotation(ObjectType("java/lang/Override", dotQualified = false))