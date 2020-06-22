import abstractor.VersionPackage
import abstractor.isMcClass
import api.*
import codegeneration.*
import codegeneration.asm.AsmCodeGenerator
import codegeneration.asm.toAsmAccess
import descriptor.JvmPrimitiveType
import descriptor.JvmType
import descriptor.ObjectType
import descriptor.ReturnDescriptor
import metautils.api.readFromJar
import metautils.signature.ArrayGenericType
import metautils.signature.GenericReturnType
import metautils.signature.GenericsPrimitiveType
import metautils.signature.TypeArgumentDeclaration
import signature.annotated
import signature.noAnnotations
import signature.toJvmType
import util.*
import java.nio.file.Path


data class AbstractionMetadata(
    val versionPackage: VersionPackage,
    val classPath: List<Path>,
    val fitToPublicApi: Boolean,
    val writeRawAsm: Boolean
)

//TODO: add tests for protected methods


//TODO: for parameter names and docs, generate a java source alongside the asm source.

object Abstractor {
    fun abstract(mcJar: Path, destDir: Path, metadata: AbstractionMetadata) {
        require(destDir.parent.exists()) { "The chosen destination path '$destDir' is not in any existing directory." }
        require(destDir.parent.isDirectory()) { "The parent of the chosen destination path '$destDir' is not a directory." }

        destDir.deleteRecursively()
        destDir.createDirectory()

        val classes = ClassApi.readFromJar(mcJar)
        val classNamesToClasses = classes.map { it.name to it }.toMap()

        // We need to add the access of api interfaces, base classes, and base api interfaces.
        // For other things in ClassEntry we just pass empty list in assumption they won't be needed.
        val additionalEntries = listAllGeneratedClasses(classes, metadata)

        val index = indexClasspath(metadata.classPath + listOf(mcJar), additionalEntries)



        for (classApi in classes
//            .filter { it.name.shortName.startsWith("TestOverrideReturnTypeChange") }
        ) {
            if (!classApi.isPublicApi) continue
            ClassAbstractor(metadata, index, classApi, classNamesToClasses).abstractClass(
                destPath = destDir,
                outerClass = null
            )
        }
    }


}

private fun listAllGeneratedClasses(
    classes: Collection<ClassApi>,
    metadata: AbstractionMetadata
): Map<QualifiedName, ClassEntry> = with(metadata.versionPackage) {
    classes.flatMap { outerClass ->
        outerClass.allInnerClassesAndThis().filter { it.isPublic || it.isProtected }.flatMap {
            val baseclass = it.name.toBaseClass() to entryJustForAccess(
                baseClassAccess(origIsInterface = it.isInterface), isStatic = it.isStatic, visibility = it.visibility
            )

            val apiInterface = it.name.toApiClass() to entryJustForAccess(
                apiInterfaceAccess(metadata), isStatic = it.isInnerClass, visibility = Visibility.Public
            )

            listOf(apiInterface, baseclass)
        }
    }.toMap()
}

private fun entryJustForAccess(access: ClassAccess, visibility: Visibility, isStatic: Boolean): ClassEntry {
    return ClassEntry(
        methods = setOf(), superInterfaces = listOf(), superClass = null,
        access = access.toAsmAccess(visibility, isStatic)
    )
}


private fun apiInterfaceAccess(metadata: AbstractionMetadata) = ClassAccess(
    isFinal = metadata.fitToPublicApi,
    variant = ClassVariant.Interface
)

//private val apiClassVisibility = ClassVisibility.Public

private fun baseClassAccess(origIsInterface: Boolean) = ClassAccess(
    isFinal = false,
    variant = if (origIsInterface) ClassVariant.Interface else ClassVariant.AbstractClass
)


private data class ClassAbstractor(
    private val metadata: AbstractionMetadata,
    private val index: ClasspathIndex,
    private val classApi: ClassApi,
    private val mcClasses: Map<QualifiedName, ClassApi>
) {
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
            visibility = classApi.visibility,
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
//        val superTypedInterface = getSuperTypeInterface()

        // If it's not a mc class then we add an asSuper() method
        val superClass = classApi.superClass?.let { if (it.isMcClass()) it.remapToApiClass() else null }

        val interfaces = classApi.superInterfaces.remapToApiClasses().appendIfNotNull(superClass)

        val classInfo = ClassInfo(
            visibility = Visibility.Public,
            access = apiInterfaceAccess(metadata),
            shortName = shortName.innermostClass(),
            typeArguments = allApiInterfaceTypeArguments(),
            superInterfaces = interfaces,
            superClass = null,
            annotations = /*classApi.annotations*/ listOf() // Translating annotations can cause compilation errors...
        ) {
            // Optimally we would like to not expose this interface at all in case this class is protected,
            // but if we declare it protected we can't reference it from the baseclass.
            // So as a compromise we declare the class as public with no body.
            //TODO: add some marker to tell users this is supposed to be protected.
            if (!classApi.isProtected) addApiInterfaceBody()
        }

        writeClass(destPath, classInfo, packageName, outerClass, isInnerClassStatic = true)
    }

    private fun writeClass(
        destPath: Path?,
        classInfo: ClassInfo,
        packageName: PackageName?,
        outerClass: GeneratedClass?,
        isInnerClassStatic: Boolean
    ) {
        val codegen = if (metadata.writeRawAsm) AsmCodeGenerator(index) else JavaCodeGenerator
        if (destPath != null) {
            codegen.writeClass(classInfo, packageName, destPath)
        } else {
            requireNotNull(outerClass)
            outerClass.addInnerClass(classInfo, isStatic = isInnerClassStatic)
        }
    }

//    private fun getSuperTypeInterface() = classApi.superClass?.let {
//        when {
//            it.isMcClass() -> it.remapToApiClass()
//            it.satisfyingTypeBoundsIsImpossible() -> null
//            else -> ClassGenericType(
//                SuperTypedPackage,
//                SimpleClassGenericType(
//                    SuperTypedName,
//                    TypeArgument.SpecificType(
//                        /*if (it.containsMcClasses()) it.remapToApiClass().type else it.type*/ it.remapToApiClass().type,
//                        wildcardType = null
//                    ).singletonList()
//                ).singletonList()
//            ).noAnnotations()
//        }
//
//    }


//    // TODO: This will do for now. A proper solution will check if:
//    // 1. This is used as a superclass in this context (this is already the case when this method is called)
//    // 2. This is a non-mc class
//    // 3. Any type arguments are bound by a type that contains themselves (e.g. T extends Enum<T>)
//    // ALTERNATIVELY------- instead of generating a SuperTyped<> interfaces just add the cast method itself to the class
//    // OR do some bytecode shenanigans if that works
//    private fun JavaClassType.satisfyingTypeBoundsIsImpossible() = type.packageName == EnumPackage
//            && type.classNameSegments[0].name == EnumName

    private fun getAllSuperMethods(): List<ClassApi.Method> = index.getSuperTypesRecursively(classApi.name)
        .mapNotNull { mcClasses[it]?.methods }.flatten()

    private fun GeneratedClass.addBaseclassBody() {
        for (method in classApi.methods) {
            if ((method.isPublic || method.isProtected) && method.isConstructor) {
                addBaseclassConstructor(method)
            }
        }

        val methodsIncludingSupers = classApi.methods + getAllSuperMethods()
        // Baseclasses don't inherit the baseclasses of their superclasses, so we need to also add all the methods
        // of the superclasses
        for (method in methodsIncludingSupers.distinctBy { it.name + it.getJvmDescriptor().classFileName }) {
            if (!method.isConstructor) {
                if (method.isPublic || method.isProtected) {
                    // The purpose of bridge methods is to get calls from mc to call the methods from the api, but when
                    // there is no mc classes involved the methods are the same as the mc ones, so when mc calls the method
                    // it will be called in the api as well (without needing a bridge method)
                    if (method.descriptorContainsMcClasses()) {
                        addBridgeMethod(method)

                        // We need to add our own override to the method because we want the bridge method
                        // to call the mc method (with a super call) by default.
                        // If we don't add this method here to override the api method, it will call the method in the api interface,
                        // which will call the bridge method - infinite recursion.
                        if (!method.isStatic || method.isProtected) addApiDeclaredMethod(method, callSuper = true)
                    }
                }
            }
        }

        for (field in classApi.fields) {
            if (field.isProtected) abstractField(field, castSelf = false)
        }

        for (innerClass in classApi.innerClasses) {
            if ((innerClass.isPublic || innerClass.isProtected) && !innerClass.isFinal) {
                copy(classApi = innerClass).createBaseclass(destPath = null, outerClass = this)
            }

            // Inner classes are constructed by their parent
            if (!innerClass.isStatic && innerClass.isProtected) {
                addInnerClassConstructor(innerClass)
            }
        }

        if (classApi.isProtected) addArrayFactory()
    }

    private fun GeneratedClass.addApiInterfaceBody() {
        addAsSuperMethod()
        for (method in classApi.methods) {
            if (method.isPublic) {
                if (method.isConstructor) addApiInterfaceFactory(method)
                else {
                    // Don't duplicate methods that are just being overriden
                    if (!method.isOverride(index, classApi)) {
                        addApiDeclaredMethod(method, callSuper = false)
                    }
                }
            }
        }

        for (field in classApi.fields) {
            if (field.isPublic) abstractField(field, castSelf = true)
        }
        for (innerClass in classApi.innerClasses) {
            if (innerClass.isPublic || innerClass.isProtected) {
                copy(classApi = innerClass).createApiInterface(destPath = null, outerClass = this)
            }

            // Inner classes are constructed by their parent
            if (!innerClass.isStatic && innerClass.isPublic) {
                addInnerClassConstructor(innerClass)
            }
        }

        addArrayFactory()
    }

    private fun ClassApi.Method.descriptorContainsMcClasses() = returnType.isMcClass()
            || parameters.values.any { it.isMcClass() }

    private fun GeneratedClass.addBridgeMethod(method: ClassApi.Method/*, delegateToApiInterface: Boolean*/) {
        // Bridges only exist for you to override methods
        if (method.isFinal || method.isStatic) return
        // Bridge methods are implementation details
        if (metadata.fitToPublicApi) return
        // The purpose of bridge methods is to get calls from mc to call the methods from the api, but when
        // there is no mc classes involved the methods are the same as the mc ones, so when mc calls the method
        // it will be called in the api as well (without needing a bridge method)
//        if (!method.returnType.isMcClass() && method.parameters.values.none { it.isMcClass() }) return

        val mcReturnType = method.returnType
        val mcClassType = classApi.asRawType()

        val methodInfo = method.apiMethodInfo(remapParameters = false) {
            val call = MethodCall.Method(
                receiver = ThisExpression,
                parameters = method.parameters.map { (name, type) ->
                    type.remapToApiClass().toJvmType() to VariableExpression(name)
                },
                name = method.name,

                returnType = mcReturnType.remapToApiClass().toJvmType(),
                // In a bridge method, call the api interface
                // (so when mc calls it it will reach the api overrides)
                //TODO: change based on delegateToApiInterface
//                owner = mcClassType.remapToApiClass().toJvmType()
                methodAccess = method.access,
                receiverAccess = classApi.access/*.copy(variant = ClassVariant.Interface)*/,
                owner = mcClassType.toJvmType()
            )

            if (!method.isVoid) {
                @Suppress("UNCHECKED_CAST")
                val returnedType = mcReturnType as AnyJavaType
                val returnedValue = call.castFromApiToMc(returnedType)
                addStatement(ReturnStatement(returnedValue))
            } else addStatement(call)
        }

        addMethod(
            methodInfo,
            name = method.name,
            isFinal = !classApi.isInterface,
            isStatic = method.isStatic,
            isAbstract = false,
            returnType = mcReturnType,
            typeArguments = method.typeArguments.remapDeclToApiClasses()
        )
    }


    private fun GeneratedClass.addApiDeclaredMethod(method: ClassApi.Method, callSuper: Boolean) {

        val mcReturnType = method.returnType
        val returnType = mcReturnType.remapToApiClass()

        val mcClassType = classApi.asRawType()

        val methodInfo = method.apiMethodInfo(remapParameters = true) {
            val call = MethodCall.Method(
                receiver = when {
                    method.isStatic -> ClassReceiver(classApi.asJvmType())
                    callSuper -> SuperReceiver
                    else -> ThisExpression.castFromApiToMc(mcClassType)
                },
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

    // Interfaces cannot have classes as their supertype, so we make a compromise by giving an asSuper() method that casts
    // to that supertype.
    private fun GeneratedClass.addAsSuperMethod() {
        val superClass = classApi.superClass ?: return
        if (!superClass.isMcClass()) {
            addMethod(
                name = "asSuper",
                visibility = Visibility.Public,
                returnType = superClass,
                parameters = mapOf(), throws = listOf(), typeArguments = listOf(),
                static = false, abstract = false, final = false
            ) {
                addStatement(ReturnStatement(ThisExpression.cast(fromType = classApi.asType(), toType = superClass)))
            }
        }
    }

//    private fun superTypedInterface(enclosedSuperClass: JavaClassType) = ClassGenericType(
//        SuperTypedPackage,
//        SimpleClassGenericType(
//            SuperTypedName,
//            TypeArgument.SpecificType(
//                enclosedSuperClass.remapToApiClass().type,
//                wildcardType = null
//            ).singletonList()
//        ).singletonList()
//    ).noAnnotations()

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
        parameters = if (remapParameters) apiParametersDeclaration(this) else parameters,
        body = body
    )

    private fun GeneratedClass.abstractField(field: ClassApi.Field, castSelf: Boolean) {
        if (field.isFinal && field.isStatic) {
            addConstant(field)
        } else {
            addGetter(field, castSelf)
        }

        if (!field.isFinal) {
            addSetter(field, castSelf)
        }
    }

    private fun GeneratedClass.addConstant(field: ClassApi.Field) {
        addField(
            name = field.name, isFinal = true, isStatic = true, visibility = field.visibility,
            type = field.type.remapToApiClass(),
            initializer = abstractedFieldExpression(field, castSelf = false).castFromMcToApi(field.type)
        )
    }

    private fun GeneratedClass.addGetter(field: ClassApi.Field, castSelf: Boolean) {
        val getterName = field.getGetterPrefix() +
                // When it starts with "is" no prefix is added so there's no need to capitalize
                if (field.name.startsWith(BooleanGetterPrefix)) field.name else field.name.capitalize()
        addMethod(
            // Add _field when getter clashes with a method of the same name
            name = if (classApi.methods.any { it.parameters.isEmpty() && it.name == getterName }) getterName + "_field"
            else getterName,
            parameters = mapOf(),
            visibility = field.visibility,
            returnType = field.type.remapToApiClass(),
            abstract = false,
            static = field.isStatic,
            final = metadata.fitToPublicApi,
            typeArguments = listOf(),
            throws = listOf()
        ) {
            val fieldAccess = abstractedFieldExpression(field, castSelf)
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

    private fun GeneratedClass.addSetter(field: ClassApi.Field, castSelf: Boolean) {
        addMethod(
            name = "set" + field.name.capitalize(),
            parameters = mapOf(field.name to field.type.remapToApiClass()),
            visibility = field.visibility,
            returnType = GenericReturnType.Void.noAnnotations(),
            abstract = false,
            static = field.isStatic,
            final = metadata.fitToPublicApi,
            typeArguments = listOf(),
            throws = listOf()
        ) {
            addStatement(
                AssignmentStatement(
                    target = abstractedFieldExpression(field, castSelf),
                    assignedValue = VariableExpression(field.name).castFromApiToMc(field.type)
                )
            )
        }
    }

    private fun abstractedFieldExpression(
        field: ClassApi.Field,
        castSelf: Boolean
    ): FieldExpression {
        val mcClassType = classApi.asRawType()
        return FieldExpression(
            receiver = if (field.isStatic) ClassReceiver(classApi.asJvmType())
            else ThisExpression.let { if (castSelf) it.castFromApiToMc(mcClassType) else it },
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
                visibility = innerClass.visibility,
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
        else -> classApi.outerClassesToThis().flatMap { it.typeArguments.remapDeclToApiClasses() }
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

    private fun <T : GenericReturnType> JavaType<T>.remapToBaseClass(): JavaType<T> =
        with(metadata.versionPackage) { remapToBaseClass() }

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