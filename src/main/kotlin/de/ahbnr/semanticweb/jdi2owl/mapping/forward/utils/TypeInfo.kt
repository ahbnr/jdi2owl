package de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils

import com.sun.jdi.*

sealed class TypeInfo(
    protected val typeInfoProvider: TypeInfoProvider
): HasRCN {
    class VoidTypeInfo(typeInfoProvider: TypeInfoProvider, val type: VoidType): TypeInfo(typeInfoProvider) {
        override val rcn: String = "void"
    }

    class PrimitiveTypeInfo(typeInfoProvider: TypeInfoProvider, val jdiType: PrimitiveType): TypeInfo(typeInfoProvider) {
        override val rcn: String = jdiType.name()
    }

    sealed class ReferenceTypeInfo(
        typeInfoProvider: TypeInfoProvider
    ): TypeInfo(typeInfoProvider) {
        abstract val binaryName: String

        abstract fun getDirectSupertypes(): Sequence<ReferenceTypeInfo>
        abstract fun getDirectSubtypes(): Sequence<ReferenceTypeInfo>

        abstract fun getAllSubtypes(): Sequence<ReferenceTypeInfo>

        sealed class CreatedType(
            typeInfoProvider: TypeInfoProvider,
        ): ReferenceTypeInfo(typeInfoProvider) {
            abstract val jdiType: ReferenceType

            fun getFieldInfo(field: Field) = FieldInfo(typeInfoProvider, this, field)
            fun getMethodInfo(jdiMethod: Method) = MethodInfo(typeInfoProvider, jdiMethod)

            override val binaryName: String
                get() = jdiType.name()

            override fun getAllSubtypes(): Sequence<ReferenceTypeInfo> {
                val directSubtypes = getDirectSubtypes()

                return directSubtypes + directSubtypes.flatMap { it.getAllSubtypes() }
            }

            sealed class ClassOrInterface: CreatedType {
                override val jdiType: ReferenceType
                override val rcn: String

                constructor(
                    typeInfoProvider: TypeInfoProvider,
                    jdiType: ReferenceType,
                    systemClassLoaderId: Long?
                ): super(typeInfoProvider) {
                    this.jdiType = jdiType

                    val classLoader = jdiType.classLoader()
                    val typeName = jdiType.name()

                    rcn =
                        if (classLoader == null)
                            typeName
                        else if (classLoader.uniqueID() == systemClassLoaderId)
                            "SysLoader-$typeName"
                        else
                            "Loader${classLoader.uniqueID()}-$typeName"
                }

                class Class(
                    typeInfoProvider: TypeInfoProvider,
                    override val jdiType: ClassType,
                    systemClassLoaderId: Long?
                ): ClassOrInterface(typeInfoProvider, jdiType, systemClassLoaderId) {
                    /**
                     * The direct supertypes of a class are its direct superclass and its directly implemented
                     * interfaces.
                     * https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.10.2
                     */
                    override fun getDirectSupertypes(): Sequence<CreatedType> =
                        (sequenceOf(jdiType.superclass()) + jdiType.interfaces().asSequence())
                            .filterNotNull() // java.lang.Object has no superclass and no superinterfaces
                            .map {
                                typeInfoProvider.getTypeInfo(it)
                            }

                    override fun getDirectSubtypes(): Sequence<CreatedType> =
                        jdiType.subclasses().asSequence().map {
                            typeInfoProvider.getTypeInfo(it)
                        }
                }

                class Interface(
                    typeInfoProvider: TypeInfoProvider,
                    override val jdiType: InterfaceType,
                    systemClassLoaderId: Long?
                ): ClassOrInterface(typeInfoProvider, jdiType, systemClassLoaderId) {
                    /**
                     * The direct supertype of an interface is only java.lang.Object,
                     * if there are no superinterfaces.
                     * Otherwise, its all direct superinterfaces.
                     *
                     * https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.10.2
                     */
                    override fun getDirectSupertypes(): Sequence<CreatedType> {
                        val jdiSuperInterfaces = jdiType.superinterfaces()

                        return if (jdiSuperInterfaces.isEmpty()) {
                            sequenceOf(typeInfoProvider.java_lang_Object)
                        }

                        else jdiSuperInterfaces
                            .asSequence()
                            .map {
                                typeInfoProvider.getTypeInfo(it)
                            }
                    }

                    override fun getDirectSubtypes(): Sequence<CreatedType> =
                        (jdiType.subinterfaces().asSequence() + jdiType.implementors().asSequence()).map {
                            typeInfoProvider.getTypeInfo(it)
                        }
                }
            }

            class ArrayType(
                typeInfoProvider: TypeInfoProvider,
                override val jdiType: com.sun.jdi.ArrayType,
                val componentType: TypeInfo
            ): CreatedType(typeInfoProvider) {
                override val rcn: String = "${componentType.rcn}[]"

                /**
                 * If the component type of an array type is a primitive type,
                 * or if the component type is java.lang.Object, then the following are the direct supertypes of the
                 * array type:
                 *
                 * * java.lang.Object
                 * * java.lang.Cloneable
                 * * java.lang.Serializable
                 *
                 * Additionally, if the component type is a reference type C,
                 * then T[] is a direct supertype, if T is a direct supertype of C
                 *
                 * https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.10.3
                 */
                override fun getDirectSupertypes(): Sequence<ReferenceTypeInfo> = when(componentType) {
                    is VoidTypeInfo -> throw RuntimeException("There can be no array type with void component type. This should never happen.")
                    is PrimitiveTypeInfo -> sequenceOf(
                        typeInfoProvider.java_lang_Object,
                        typeInfoProvider.java_lang_Cloneable,
                        typeInfoProvider.java_io_Serializable
                    )
                    is ReferenceTypeInfo -> {
                        val arraySupertypes = componentType
                            .getDirectSupertypes()
                            .map {
                                val arrayTypeName = "${it.binaryName}[]"

                                val classLoader: ClassLoaderReference? =
                                    (it as? CreatedType)?.jdiType?.classLoader()

                                typeInfoProvider
                                    .getPreparedTypeInfoByName(arrayTypeName, classLoader)
                                    ?: typeInfoProvider.getNotYetLoadedTypeInfo(arrayTypeName)
                            }

                        if (componentType.binaryName == "java.lang.Object") {
                            sequenceOf(
                                typeInfoProvider.java_lang_Object,
                                typeInfoProvider.java_lang_Cloneable,
                                typeInfoProvider.java_io_Serializable
                            ) + arraySupertypes
                        }
                        else arraySupertypes
                    }
                }

                override fun getDirectSubtypes(): Sequence<ReferenceTypeInfo> =
                    when(componentType) {
                        is VoidTypeInfo -> throw RuntimeException("There can be no array type with void component type. This should never happen.")
                        is PrimitiveTypeInfo -> emptySequence()
                        is ReferenceTypeInfo ->
                            componentType
                                .getDirectSubtypes()
                                .map {
                                    val arrayTypeName = "${it.binaryName}[]"

                                    val classLoader: ClassLoaderReference? =
                                        (it as? CreatedType)?.jdiType?.classLoader()

                                    typeInfoProvider
                                        .getPreparedTypeInfoByName(arrayTypeName, classLoader)
                                        ?: typeInfoProvider.getNotYetLoadedTypeInfo(arrayTypeName)
                                }
                    }
            }
        }

        class UnpreparedType(
            typeInfoProvider: TypeInfoProvider,
            override val binaryName: String
        ): ReferenceTypeInfo(typeInfoProvider) {
            override val rcn: String = "Unprepared-$binaryName"

            // Direct super-/subtypes are unknown for not-yet loaded types
            override fun getDirectSupertypes(): Sequence<ReferenceTypeInfo> = emptySequence()
            override fun getDirectSubtypes(): Sequence<ReferenceTypeInfo> = emptySequence()
            override fun getAllSubtypes(): Sequence<ReferenceTypeInfo> = emptySequence()
        }
    }
}