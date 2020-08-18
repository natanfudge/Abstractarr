package abstractor

import codegeneration.Public
import metautils.api.*
import metautils.codegeneration.*
import metautils.codegeneration.asm.AsmCodeGenerator
import metautils.signature.*
import metautils.types.JvmPrimitiveTypes
import metautils.types.JvmType
import metautils.util.*
import java.nio.file.Path


internal data class ClassAbstractor(
    private val metadata: AbstractionMetadata,
    private val index: ClasspathIndex,
    private val classApi: ClassApi,
    private val mcClasses: Map<QualifiedName, ClassApi>,
//    private val stubClasses: Set<QualifiedName>,
    private val abstractedClasses: Set<QualifiedName>
) {
    fun abstractClass(destPath: Path) {
        check(!classApi.isInnerClass)
        check(classApi.visibility == ClassVisibility.Public)

        createApiInterface(outerClass = null, destPath = destPath)
        if (!classApi.isFinal) createBaseclass(null, destPath)
    }
    // No need to add I for inner classes

    private fun apiClassName(outerClass: GeneratedClass?, nameMap: (QualifiedName) -> QualifiedName) =
        if (outerClass == null) nameMap(classApi.name) else classApi.name

    private fun JavaClassType.isAccessibleAsAbstractedApi(): Boolean {
        if (!isMcClass()) return true
        val classApi = mcClasses.getValue(type.toJvmQualifiedName())
        return classApi.isPublicApiAsOutermostMember && classApi.name in abstractedClasses
    }

    // Sometimes mc stupidly inherits non-public classes in public classes so we need to filter them out
    private fun superInterfacesAccessibleAsAbstractedApi() =
        classApi.superInterfaces.filter { it.isAccessibleAsAbstractedApi() }

    private fun createBaseclass(outerClass: GeneratedClass?, destPath: Path?) = with(metadata.versionPackage) {
        if (!metadata.selector.classes(classApi).addInBaseclass) return@with

        val (packageName, shortName) = apiClassName(outerClass) { it.toBaseClass() }

        val mcClass = classApi.asType()

        val additionalInterfaces = metadata.interfacesbase[classApi.name.toSlashString(
            true
        )]?.map { JavaType.fromRawClassName(it) } ?: listOf()

        val interfaces = listOf(
            mcClass.remapToApiClass().pushAllTypeArgumentsToInnermostClass()
        ) + additionalInterfaces
            // With interfaces, baseclasses need to have the mc class as their super INTERFACE and not the super CLASS
            // Obv mc classes are not exposed to the user
            .applyIf(classApi.isInterface && !metadata.fitToPublicApi) { it + mcClass }
//        +
//                publicSuperInterfaces().remapToApiClasses()


        val classInfo = ClassInfo(
            visibility = classApi.visibility,
            access = baseClassAccess(classApi.isInterface),
            shortName = shortName.innermostClass(),
            typeArguments = baseClassTypeArguments(),
            superInterfaces = interfaces,
            superClass = when {
                classApi.isInterface -> null
                metadata.fitToPublicApi -> getClosestNonMcSuperclass()?.remapToApiClass()
                else -> classApi.asRawType()
            },
            annotations = classApi.annotations
        ) {
            getJavadoc(classApi.documentable())?.let { addJavadoc(it) }
            addBaseclassBody()
        }

        writeClass(destPath, classInfo, packageName, outerClass, isInnerClassStatic = classApi.isStatic)
    }

    /**
     * destPath == null and outerClass != null when it's an inner class
     */
    private fun createApiInterface(outerClass: GeneratedClass?, destPath: Path?) = with(metadata.versionPackage) {
        val (packageName, shortName) = apiClassName(outerClass) { it.toApiClass() }

        // If it's not a mc class then we add an asSuper() method
        val superClass = classApi.superClass?.let {
            if (it.isMcClass() && it.isAccessibleAsAbstractedApi()) it.remapToApiClass() else null
        }

        val additionalInterfaces = metadata.iinterfaces[classApi.name.toSlashString(
            true
        )]?.map { JavaType.fromRawClassName(it) } ?: listOf()
        val interfaces = superInterfacesAccessibleAsAbstractedApi().remapToApiClasses().appendIfNotNull(superClass) +
                additionalInterfaces

        val classInfo = ClassInfo(
            visibility = Visibility.Public,
            access = apiInterfaceAccess(metadata),
            shortName = shortName.innermostClass(),
            typeArguments = allApiInterfaceTypeArguments(),
            superInterfaces = interfaces,
            superClass = null,
            annotations = classApi.annotations
        ) {
            // Optimally we would like to not expose this interface at all in case this class is protected,
            // but if we declare it protected we can't reference it from the baseclass.
            // So as a compromise we declare the class as public with no body.
            if (!classApi.isProtected) addApiInterfaceBody()
            else addJavadoc("This is originally a protected class")
        }

        writeClass(destPath, classInfo, packageName, outerClass, isInnerClassStatic = true)
    }

    private fun codegen() = if (metadata.writeRawAsm) AsmCodeGenerator(index) else JavaCodeGenerator

    private fun writeClass(
        destPath: Path?,
        classInfo: ClassInfo,
        packageName: PackageName,
        outerClass: GeneratedClass?,
        isInnerClassStatic: Boolean
    ) {
        val codegen = codegen()
        if (destPath != null) {
            codegen.writeClass(classInfo, packageName, destPath)
        } else {
            requireNotNull(outerClass)
            outerClass.addInnerClass(classInfo, isStatic = isInnerClassStatic)
        }
    }

    private fun ClassApi.Field.isAccessibleAsAbstractedApi() = type.getContainedNamesRecursively().all {
        val classApi = mcClasses[it] ?: return@all true
        classApi.isPublicApi && classApi.name in abstractedClasses
    }

    private fun ClassApi.Method.isAccessibleAsAbstractedApi() = getContainedNamesRecursively().all {
        val classApi = mcClasses[it] ?: return@all true
        classApi.isPublicApi && classApi.name in abstractedClasses
    }


    private data class BaseclassMethodsAddedInfo(
        val apiMethods: List<ClassApi.Method>,
        val bridgeMethods: List<ClassApi.Method>
    )

    private fun methodsAddedInBaseclass(allMethods: List<ClassApi.Method>): BaseclassMethodsAddedInfo {
        val distinctMethods = allMethods.distinctBy { it.locallyUniqueIdentifier }
        val relevantMethods = distinctMethods.filter {
            // Constructors are handled separately
            !it.isConstructor && it.isAccessibleAsAbstractedApi()
                    && metadata.selector.methods(it).addInBaseclass
        }

        val bridgeMethods = relevantMethods.filter { method ->
            val containsMc = method.descriptorContainsMcClasses()
            if (!method.isPublic && !method.isProtected) return@filter false

            // The purpose of bridge methods is to get calls from mc to call the methods from the api, but when
            // there is no mc classes involved the methods are the same as the mc ones, so when mc calls the method
            // it will be called in the api as well (without needing a bridge method)
            return@filter containsMc
        }

        // Baseclasses don't inherit the baseclasses of their superclasses, so we need to also add all the methods
        // of the superclasses
        val apiMethods = distinctMethods.filter { method ->
            //TODO: code here seems weird,  why not just use relevantMethods?

            // Constructors are handled separately
            if (method.isConstructor || !method.isAccessibleAsAbstractedApi()
                || !metadata.selector.methods(method).addInBaseclass
            ) return@filter false
            val containsMc = method.descriptorContainsMcClasses()

            return@filter if (method.isProtected || (method.isPublic && classApi.isProtected)) {
                // Multiple things to consider here.
                // 1. In the implementation jar (fitToPublicApi = false), there's no need to add methods that don't contain
                // mc classes, because when an api user calls a method without mc classes, the jvm will just look up
                // to the mc class and call it. But when a mc class is in the descriptor, the api method descriptor
                // will get remapped to have api classes, so the call will no longer be valid for the originally declared
                // mc methods.
                // We also actively cannot add a "containsMc = false" method when mc declares it as final,
                // because it will be considered as an override for the mc declared method. (Which is not allowed for final methods)
                // 2. In the api jar (fitToPublicApi = true), the mc methods are not seen by the user so we need to also add
                // "containsMc = false" methods. There is no problem of overriding because it's not passed through a JVM verifier.
                if (!metadata.fitToPublicApi) containsMc
                else true
            } else if (method.isPublic) {
                // We need to add our own override to the method because we want the bridge method
                // to call the mc method (with a super call) by default.
                // If we don't add this method here to override the api method, it will call the method in the api interface,
                // which will call the bridge method - infinite recursion.
                // This override is not needed to be seen by the user.

                // We want to avoid users creating lambdas of api interfaces because they are not meant to be implemented, and make them make lambdas of the baseclasses instead.
                // So when the class is a SAM Interface, we make the api interface not have a single abstract method, and we make the baseclass have a single abstract method.
                if (metadata.fitToPublicApi) classApi.isSamInterface() && method.isAbstract
                else !method.isStatic && containsMc
            } else false
        }
        return BaseclassMethodsAddedInfo(apiMethods, bridgeMethods)
    }

    private fun GeneratedClass.addBaseclassBody() {
        for (method in classApi.methods) {
            if ((method.isPublic || method.isProtected) && method.isConstructor) {
                addBaseclassConstructor(method)
            }
        }

        val allMethods = classApi.getAllMinecraftMethodsIncludingSupers()
        val (apiMethods, bridgeMethods) = methodsAddedInBaseclass(allMethods)
        for (method in bridgeMethods) addBridgeMethod(method)
        for (method in apiMethods) {
            addApiDeclaredMethod(method, callSuper = true, forceBody = false, finalIsAllowed = true)
        }

        //TODO: need to make sure we test against existing methods when the max possible is added i.e.
        //
        val existingMethods = (allMethods +
                // We don't want to override final methods accidentally with a getter/setter
                allMethods.filter { it.isFinal })
            .distinctBy { it.locallyUniqueIdentifier }
        for (field in classApi.fields) {
            if (field.isProtected && field.isAccessibleAsAbstractedApi()
                && metadata.selector.fields(field).addInBaseclass
            ) {
                abstractField(field, castSelf = false, existingMethods = existingMethods)
            }
        }

        for (innerClass in classApi.innerClasses) {
            if ((innerClass.isPublic || innerClass.isProtected) && !innerClass.isFinal) {
                copy(classApi = innerClass).createBaseclass(outerClass = this, destPath = null)
            }

            // soft to do: make it possible to construct protected inner classes somehow, right
            // now it will run into access errors
//            if (!innerClass.isStatic && innerClass.isProtected) {
//                addInnerClassConstructor(innerClass)
//            }
        }

        if (classApi.isProtected) addArrayFactory()
    }

    private data class ApiInterfaceMethodsAddedInfo(
        val apiMethods: List<ClassApi.Method>,
        val factories: List<ClassApi.Method>
    )


    private fun methodsAddedInApiInterface(): ApiInterfaceMethodsAddedInfo {
        val relevantMethods = classApi.methods.filter {
            it.isPublic && it.isAccessibleAsAbstractedApi()
                    && metadata.selector.methods(it).addInInterface
        }

        val factories = relevantMethods.filter { it.isConstructor }
        val apiMethods = relevantMethods.filter { method ->
            if (method.isConstructor) return@filter false

            // Don't duplicate methods that are just being overriden in the impl jar.
            // In the api jar we need to make it obvious there's no need to override that method yourself.
            return@filter !method.isOverride(index, classApi)
                    || (metadata.fitToPublicApi && method.isOnlyImplementingOverride(index, classApi))

        }

        return ApiInterfaceMethodsAddedInfo(apiMethods, factories)
    }

    private fun GeneratedClass.addApiInterfaceBody() {
        addAsSuperMethod()

        val (apiMethods, factories) = methodsAddedInApiInterface()
        for (factory in factories) addApiInterfaceFactory(factory)
        for (method in apiMethods) {
            addApiDeclaredMethod(
                method,
                callSuper = false,
                forceBody = classApi.isSamInterface(),
                finalIsAllowed = false
            )
        }

        for (field in classApi.fields) {
            if (field.isPublic && field.isAccessibleAsAbstractedApi()
                && metadata.selector.fields(field).addInInterface
            ) {
                abstractField(field, castSelf = true, existingMethods = apiMethods)
            }
        }

        addBareBonesBody()
        for (innerClass in classApi.innerClasses) {
            // Inner classes are constructed by their parent
            if (!innerClass.isStatic && innerClass.isPublic) {
                addInnerClassConstructor(innerClass)
            }
        }

        addArrayFactory()
    }

    private fun GeneratedClass.addBareBonesBody() {
        getJavadoc(classApi.documentable())?.let { addJavadoc(it) }
        for (innerClass in classApi.innerClasses) {
            if (innerClass.name in abstractedClasses) {
                copy(classApi = innerClass).createApiInterface(outerClass = this, destPath = null)
            }
        }
    }

    private fun ClassApi.Method.descriptorContainsMcClasses() = returnType.isMcClass()
            || parameters.values.any { it.isMcClass() }

    private fun GeneratedClass.addBridgeMethod(method: ClassApi.Method/*, delegateToApiInterface: Boolean*/) {
        // Bridges only exist for you to override methods
        if (method.isFinal || method.isStatic) return
        // Bridge methods are implementation details
        if (metadata.fitToPublicApi) return

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
                // In a bridge method, call the baseclass method of the same name that overrides the api method for this method
                // (so when mc calls it it will reach the api overrides)
                methodAccess = method.access,
                receiverAccess = classApi.access/*.copy(variant = ClassVariant.Interface)*/,
                owner = with((metadata.versionPackage)) { mcClassType.toJvmType().map { it.toBaseClass() } }
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
            typeArguments = method.typeArguments.remapToApiClasses()
        )
    }


    private fun GeneratedClass.addApiDeclaredMethod(
        method: ClassApi.Method, callSuper: Boolean, forceBody: Boolean,
        // Abstract classes can have final methods, interfaces can't
        finalIsAllowed: Boolean
    ) {

        val mcReturnType = method.returnType
        val returnType = mcReturnType.remapToApiClass()

        val mcClassType = classApi.asRawType()

        val noBody = metadata.fitToPublicApi && method.isAbstract && !forceBody

        val methodInfo = method.apiMethodInfo(remapParameters = true) {
            getJavadoc(method.documentable())?.let { addJavadoc(it) }
            if (noBody) return@apiMethodInfo
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
            name = method.name.applyIf(method.conflictsWithFactoryMethod()) { it + "_method" },
            isFinal = method.isFinal && (finalIsAllowed || metadata.fitToPublicApi),
            isStatic = method.isStatic,
            isAbstract = noBody,
            returnType = returnType,
            typeArguments = method.typeArguments.remapToApiClasses()
        )

    }

    private fun ClassApi.Method.conflictsWithFactoryMethod(): Boolean {
        if (name != factoryMethodName) return false
        val params = getJvmParameters()
        return classApi.methods.any { it.isConstructor && it.getJvmParameters() == params }
    }


    private fun GeneratedClass.addBaseclassConstructor(method: ClassApi.Method) {
        // Abstract classes/interfaces can't be constructed directly
        if (classApi.isInterface) return

        val mcClassType = classApi.asRawType()

        val methodInfo = method.apiMethodInfo(remapParameters = true) {
            getJavadoc(method.documentable())?.let { addJavadoc(it) }
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
                parameters = listOf(), throws = listOf(), typeArguments = listOf(),
                static = false, abstract = false, final = false
            ) {
                addStatement(ReturnStatement(ThisExpression.cast(fromType = classApi.asType(), toType = superClass)))
            }
        }
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
            getJavadoc(method.documentable())?.let { addJavadoc(it) }
            val returnedValue = MethodCall.Constructor(
                constructing = mcClassType,
                parameters = apiPassedParameters(method),
                receiver = null
            ).castFromMcToApi(mcReturnType)
            addStatement(ReturnStatement(returnedValue))
        }

        addMethod(
            methodInfo,
            name = factoryMethodName,
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
        parameters = parameters.mapIndexed { i, (name, type) ->
            val finalType = type.applyIf(remapParameters) { it.remapToApiClass() }
            ParameterInfo(name, finalType, javadoc = getJavadoc(parameterDocumentable(i)))
        },
        body = body
    )


    private fun GeneratedClass.abstractField(
        field: ClassApi.Field,
        castSelf: Boolean,
        existingMethods: List<ClassApi.Method>
    ) {
//        if (field.isFinal && field.isStatic) {
//            addConstant(field)
//        } else {
        addGetter(field, castSelf, existingMethods)
//        }

        if (!field.isFinal) {
            addSetter(field, castSelf, existingMethods)
        }
    }

    private fun GeneratedClass.addConstant(field: ClassApi.Field) {
        addField(
            name = field.name, isFinal = true, isStatic = true, visibility = field.visibility,
            type = field.type.remapToApiClass(),
            initializer = abstractedFieldExpression(field, castSelf = false).castFromMcToApi(field.type)
        )
    }

    private fun GeneratedClass.addGetter(
        field: ClassApi.Field,
        castSelf: Boolean,
        existingMethods: List<ClassApi.Method>
    ) {
        val getterName = field.getGetterPrefix() +
                // When it starts with "is" no prefix is added so there's no need to capitalize
                if (field.name.startsWith(BooleanGetterPrefix)) field.name else field.name.capitalize()
        addMethod(
            // Add _field when getter clashes with a method of the same name
            name = getterName.applyIf(getterClashesWithMethod(getterName, existingMethods)) { it + "_field" },
            parameters = listOf(),
            visibility = field.visibility,
            returnType = field.type.remapToApiClass(),
            abstract = false,
            static = field.isStatic,
            final = metadata.fitToPublicApi && !field.isStatic,
            typeArguments = listOf(),
            throws = listOf()
        ) {
            val fieldAccess = abstractedFieldExpression(field, castSelf)
            addStatement(
                ReturnStatement(
                    fieldAccess.castFromMcToApi(field.type)
                )
            )
            getJavadoc(field.documentable())?.let { addJavadoc(it) }
        }
    }

    private fun getterClashesWithMethod(getterName: String, existingMethods: List<ClassApi.Method>) =
        existingMethods.any { it.parameters.isEmpty() && it.name == getterName }


    private fun setterClashesWithMethod(
        setterName: String,
        setterType: JvmType,
        existingMethods: List<ClassApi.Method>
    ): Boolean {
        return existingMethods
            .any { it.parameters.size == 1 && it.name == setterName && it.parameters[0].second.toJvmType() == setterType }
    }

    private fun ClassApi.Field.getGetterPrefix(): String =
        if (type.type.let { it is GenericsPrimitiveType && it.primitive == JvmPrimitiveTypes.Boolean }) {
            if (name.startsWith(BooleanGetterPrefix)) "" else BooleanGetterPrefix
        } else "get"

    private fun GeneratedClass.addSetter(
        field: ClassApi.Field,
        castSelf: Boolean,
        existingMethods: List<ClassApi.Method>
    ) {
        val setterName = "set" + field.name.capitalize()
        addMethod(
            name = setterName
                .applyIf(
                    setterClashesWithMethod(
                        setterName,
                        field.type.toJvmType(),
                        existingMethods
                    )
                ) { it + "_field" },
            parameters = listOf(
                ParameterInfo(
                    field.name,
                    field.type.remapToApiClass(),
                    getJavadoc(field.documentable())
                )
            ),
            visibility = field.visibility,
            returnType = VoidGenericReturnType.noAnnotations(),
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
                final = metadata.fitToPublicApi,
                abstract = false,
                returnType = constructedInnerClass.remapToApiClass().pushAllTypeArgumentsToInnermostClass()
                    .copy(annotations = listOf(NotNullAnnotation)),
                parameters = constructor.parameters.mapIndexed { i, (name, type) ->
                    ParameterInfo(
                        name, type.remapToApiClass(),
                        getJavadoc(constructor.parameterDocumentable(innerClass, i))
                    )
                },
                typeArguments = innerClass.typeArguments.remapToApiClasses(),
                throws = constructor.throws
            ) {
                getJavadoc(constructor.documentable())?.let { addJavadoc(it) }
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

    private fun GeneratedClass.addArrayFactory() {
        addMethod(
            name = if (existsMethodWithSameDescriptorAsArrayFactory()) "${ArrayFactoryName}_factory" else ArrayFactoryName,
            visibility = Visibility.Public,
            parameters = listOf(
                ParameterInfo(ArrayFactorySizeParamName, GenericsPrimitiveType.Int.noAnnotations(), javadoc = null)
            ),
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
            )
        }
    }

//    private fun GeneratedClass.addMethodIfAllowed(
//            methodInfo: MethodInfo,
//            isStatic: Boolean, isFinal: Boolean, isAbstract: Boolean,
//            typeArguments: List<TypeArgumentDeclaration>,
//            name: String,
//            returnType: JavaReturnType
//    ){
//
//    }
//
//    private fun GeneratedClass.addMethodIfAllowed(
//            visibility: Visibility,
//            parameters: List<ParameterInfo>,
//            throws: List<JavaThrowableType>,
//            static: Boolean,
//            final: Boolean,
//            abstract: Boolean,
//            typeArguments: List<TypeArgumentDeclaration>,
//            name: String,
//            returnType: JavaReturnType,
//            body: GeneratedMethod.() -> Unit
//    ): Unit = addMethodIfAllowed(
//            MethodInfo(visibility, parameters, throws, body),
//            static, final, abstract, typeArguments, name, returnType
//    )


////////////// JAVADOCS //////////////

    private fun ClassApi.documentable() = Documentable.Class(name)
    private fun ClassApi.Method.documentable() = documentable(classApi)
    private fun ClassApi.Method.documentable(classIn: ClassApi) =
        Documentable.Method(classIn.documentable(), name, getJvmDescriptor())

    private fun ClassApi.Method.parameterDocumentable(index: Int) = parameterDocumentable(classApi, index)
    private fun ClassApi.Method.parameterDocumentable(classIn: ClassApi, index: Int) =
        Documentable.Parameter(documentable(classIn), index)

    private fun ClassApi.Field.documentable() =
        Documentable.Field(classApi.documentable(), name)

    private fun getJavadoc(documentable: Documentable) = metadata.javadocs.getDoc(documentable)?.plus("\n")

////////// TYPE VARIABLE INLINING //////////

//    class Foo<T> : Bar<String>()
//    open class Bar<T1> {
//        fun <T> bar(t1: T1, t: T) {}
//    }

    private fun ClassApi.getAllMinecraftMethodsIncludingSupers(): List<ClassApi.Method> {
        val superTypes = superInterfaces.appendIfNotNull(superClass)

        return this.methods +
                superTypes.mapNotNull { superType -> mcClasses[superType.type.toJvmQualifiedName()]?.let { superType to it } }
                    .flatMap { (superType, superTypeApi) ->
                        // [<T> bar(t1: T1, t: T)]
                        superTypeApi.getAllMinecraftMethodsIncludingSupers().map {
                            // [<T_OVERRIDE> bar(t1: T1, t: T_OVERRIDE)]
                            it.resolveTypeVariableNameConflicts(this)
                                // [<T_OVERRIDE> bar(t1: String, t: T_OVERRIDE)]
                                .inlineContainedTypes(superType, superTypeApi.typeArguments)
                        }
                    }
    }

    // When a class has <T> defined, and the method defines a type argument <T>, rename <T> of the method to <T_OVERRIDE>
    private fun ClassApi.Method.resolveTypeVariableNameConflicts(classApi: ClassApi): ClassApi.Method {
        val conflictingNames = typeArguments.map { it.name }
            .filter { methodArg -> classApi.typeArguments.any { it.name == methodArg } }
        return mapTypeVariables {
            if (it.name in conflictingNames) it.copy(name = it.name + ConflictingTypeVariableSuffix) else it
        }.copy(typeArguments = typeArguments.map {
            if (it.name in conflictingNames) it.copy(name = it.name + ConflictingTypeVariableSuffix) else it
        })
    }

    // For cases when mc extends a non-mc class and implements some interface with it, we need to put in the dev
// jar that the baseclass has that mc class as a super interface so that it doesn't seem like the methods were
// not implemented.
    private fun getClosestNonMcSuperclass(): JavaClassType? {
        var classApiOfCurrent: ClassApi? = classApi
        var current: JavaClassType? = null
        do {
            current = (classApiOfCurrent?.superClass ?: return null)
                .let { superClass ->
                    if (current != null) {
                        // See comments for inlinePassedTypeArguments()
                        // current = Bar<String,T>
                        // classApiOfCurrent = ClassApi(Bar<T1,T2>)
                        // superClass = ArrayList<T2>
                        // superClassTypeArgs = <T1,T2>
                        val superClassTypeArgs = classApiOfCurrent!!.typeArguments
                        superClass.inlineTypeVariables(current!!, superClassTypeArgs)
                    } else superClass
                }

            classApiOfCurrent = mcClasses[current.type.toJvmQualifiedName()]
        } while (classApiOfCurrent != null)  // If it's null then it means we've reached a non-mc superclass

        return current
    }


    // When taking class Foo<T> extends Bar<String, T>, and class Bar<T1,T2> extends ArrayList<T2>,
    // it will convert ArrayList<T2> to ArrayList<T> so it can be used as a superclass of Foo<T>
    // ArrayList<T2> is passed as `this`, Bar<String,T> is passed as first param and <T1,T2> is passed as second param.

    // Complicated process, so in comments we visualize the values of variables in accordance to the example
    private fun TypeVariable.inlineTypeVariable(
        superClassType: JavaClassType, superClassTypeArgDecls: List<TypeArgumentDeclaration>
    ): GenericType {
        // this = TypeVariable(T2)
        // name = T2
        // superClassTypeArgDecls = <T1,T2>
        // indexOfOldName = 1
        val indexOfOldName = superClassTypeArgDecls.indexOfFirst { it.name == name }
        // If it's not defined in the superClassTypeArgDecls it means it was defined somewhere else like the subclass or in a method,
        // which means there's no need to replace it with something else.
        if (indexOfOldName == -1) return this

        // innerMostClassPassedArgs = <String, T>
        val innerMostClassPassedArgs = superClassType.type.classNameSegments.last().typeArguments
        // inlinedVariableAsArgument = TypeArgument(T)
        val inlinedVariableAsArgument = innerMostClassPassedArgs[indexOfOldName]
        // You can't pass wildcards as a superclass type argument
        check(inlinedVariableAsArgument is TypeArgument.SpecificType && inlinedVariableAsArgument.wildcardType == null)
        // innerMostClassPassedArgs[indexOfOldName] = T
        return inlinedVariableAsArgument.type
    }

    private fun <T : GenericReturnType> JavaType<T>.inlineTypeVariables(
        superClassType: JavaClassType, superClassTypeArgDecls: List<TypeArgumentDeclaration>
    ) = mapTypeVariables { it.inlineTypeVariable(superClassType, superClassTypeArgDecls) }


    private fun ClassApi.Method.mapTypeVariables(
        mapper: (TypeVariable) -> GenericType
    ) = copy(returnType = returnType.mapTypeVariables(mapper),
        parameters = parameters.mapValues { it.mapTypeVariables(mapper) },
        throws = throws.map { it.mapTypeVariables(mapper) },
        typeArguments = typeArguments.map { it.mapTypeVariables(mapper) }
    )

    private fun ClassApi.Method.inlineContainedTypes(
        superClassType: JavaClassType, superClassTypeArgDecls: List<TypeArgumentDeclaration>
    ) = mapTypeVariables { it.inlineTypeVariable(superClassType, superClassTypeArgDecls) }

    private fun allApiInterfaceTypeArguments() = with(metadata.versionPackage) {
        allApiInterfaceTypeArguments(classApi)
    }

////////// REMAPPING /////////

    private fun <T : NameMappable<T>> T.remapToApiClass() = with(metadata.versionPackage) { remapToApiClass() }
    private fun <T : NameMappable<T>> List<T>.remapToApiClasses() = map { it.remapToApiClass() }

//    private fun <T : GenericReturnType> JavaType<T>.remapToApiClass(): JavaType<T> =
//
//
//    private fun <T : GenericReturnType> T.remapToApiClass(): T =
//        with(metadata.versionPackage) { remapToApiClass() }
//
//    private fun <T : NameMappable<T>> T.remapToApiClass(): T =
//        with(metadata.versionPackage) { remapToApiClass() }
//
//    private fun List<TypeArgumentDeclaration>.remapDeclToApiClasses(): List<TypeArgumentDeclaration> =
//        with(metadata.versionPackage) { remapDeclToApiClasses() }

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

//// soft to do: not sure what to do when a generic class uses type arguments that were not abstracted
//internal fun VersionPackage.allApiInterfaceTypeArguments(classApi: ClassApi): List<TypeArgumentDeclaration>{
//    val arguments = allUnmappedApiInterfaceTypeArguments(classApi)
//    return arguments.
//}
//
////private fun TypeArgumentDeclaration.filterOutBoundsNotAccessibleFromAbstractedApi()
////= when {
////    classApi.isStatic -> classApi.typeArguments.remapDeclToApiClasses()
////    else -> classApi.outerClassesToThis().flatMap { it.typeArguments.remapDeclToApiClasses() }
////}

// soft to do: not sure what to do when a generic class uses type arguments that were not abstracted
internal fun VersionPackage.allApiInterfaceTypeArguments(classApi: ClassApi): List<TypeArgumentDeclaration> = when {
    classApi.isStatic -> classApi.typeArguments.remapDeclToApiClasses()
    else -> classApi.outerClassesToThis().flatMap { it.typeArguments.remapDeclToApiClasses() }
}

//// soft to do: not sure what to do when a generic class uses type arguments that were not abstracted
//internal fun VersionPackage.allUnmappedApiInterfaceTypeArguments(classApi: ClassApi): List<TypeArgumentDeclaration> = when {
//    classApi.isStatic -> classApi.typeArguments
//    else -> classApi.outerClassesToThis().flatMap { it.typeArguments}
//}

private const val ConflictingTypeVariableSuffix = "_OVERRIDE"
private const val factoryMethodName = "create"

private const val BooleanGetterPrefix = "is"
private const val ArrayFactoryName = "array"
private const val ArrayFactorySizeParamName = "size"
private val NotNullAnnotation = JavaAnnotation.fromRawJvmClassName("org/jetbrains/annotations/NotNull")