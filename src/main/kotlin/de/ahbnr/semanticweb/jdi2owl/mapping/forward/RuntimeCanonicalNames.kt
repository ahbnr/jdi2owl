package de.ahbnr.semanticweb.jdi2owl.mapping.forward

import com.sun.jdi.ArrayType
import com.sun.jdi.ClassNotLoadedException
import com.sun.jdi.ClassType
import com.sun.jdi.Field
import com.sun.jdi.InterfaceType
import com.sun.jdi.Method
import com.sun.jdi.PrimitiveType
import com.sun.jdi.ReferenceType
import com.sun.jdi.ThreadReference
import com.sun.jdi.Type
import com.sun.jdi.VoidType
import de.ahbnr.semanticweb.jdi2owl.Logger
import de.ahbnr.semanticweb.jdi2owl.debugging.mirrors.ClassLoaderMirror
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.MethodInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface HasRCN {
    val rcn: String
}

class FieldInfo(val typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType, val jdiField: Field): HasRCN {
    init {
        if (typeInfo.jdiType != jdiField.declaringType())
            throw java.lang.IllegalArgumentException("The JDI field instance does not belong to the given declaring type.")
    }

    override val rcn: String = "${typeInfo.rcn}.${jdiField.name()}"
}

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

        sealed class CreatedType(
            typeInfoProvider: TypeInfoProvider,
        ): ReferenceTypeInfo(typeInfoProvider) {
            abstract val jdiType: ReferenceType

            fun getFieldInfo(field: Field) = FieldInfo(this, field)
            fun getMethodInfo(jdiMethod: Method) = MethodInfo(typeInfoProvider, jdiMethod)

            override val binaryName: String
                get() = jdiType.name()

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

                                typeInfoProvider
                                    .getPreparedTypeInfoByName(arrayTypeName)
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
            }
        }

        class NotYetLoadedType(
            typeInfoProvider: TypeInfoProvider,
            override val binaryName: String
        ): ReferenceTypeInfo(typeInfoProvider) {
            override val rcn: String = "NotYetLoaded-$binaryName"

            // Direct supetypes are unknown for not-yet loaded types
            override fun getDirectSupertypes(): Sequence<ReferenceTypeInfo> = emptySequence()
        }
    }
}

class TypeInfoProvider(mainThread: ThreadReference) {
    private val jdiTypeToTypeInfo = mutableMapOf<Type, TypeInfo>()
    private val systemClassLoaderId: Long? = ClassLoaderMirror.Static
        .getBuiltInClassLoaderType(mainThread)
        ?.getSystemClassLoader()
        ?.uniqueID()

    private val vm = mainThread.virtualMachine()

    // FIXME: This is not unique, considering class loaders
    val java_lang_Object: TypeInfo.ReferenceTypeInfo.CreatedType.ClassOrInterface.Class
    val `java_lang_Object%5B%5D`: TypeInfo.ReferenceTypeInfo
    val java_lang_Cloneable: TypeInfo.ReferenceTypeInfo
    val java_io_Serializable: TypeInfo.ReferenceTypeInfo

    init {
        fun getReferenceTypeInfo(binaryName: String) =
            getPreparedTypeInfoByName(binaryName)
                ?: getNotYetLoadedTypeInfo(binaryName)

        java_lang_Object = getPreparedTypeInfoByName("java.lang.Object")
            as? TypeInfo.ReferenceTypeInfo.CreatedType.ClassOrInterface.Class
            ?: throw RuntimeException("java.lang.Object must always be loaded. We do not support early VM states where it is not. This should never happen.")
        `java_lang_Object%5B%5D` = getReferenceTypeInfo("java.lang.Object[]")
        java_lang_Cloneable = getReferenceTypeInfo("java.lang.Cloneable")
        java_io_Serializable = getReferenceTypeInfo("java.io.Serializable")
    }

    /**
     * Returns ReferenceTypeInfo instance for the type with the given name,
     * if the type has at least been prepared, in the case of a non-array type,
     * or if the type has at least been created, in the case of array types.
     *
     * FIXME: This is actually not unique, considering class loaders
     */
    fun getPreparedTypeInfoByName(binaryName: String): TypeInfo.ReferenceTypeInfo.CreatedType? =
        vm
            .classesByName(binaryName)
            .firstOrNull()
            ?.let {
                getTypeInfo(it)
            }

    fun getTypeInfo(type: Type): TypeInfo {
        jdiTypeToTypeInfo[type]?.let {
            return it
        }

        val typeInfo = when (type) {
            is PrimitiveType -> TypeInfo.PrimitiveTypeInfo(this, type)
            is ReferenceType -> when (type) {
                is ClassType ->
                    TypeInfo.ReferenceTypeInfo.CreatedType.ClassOrInterface.Class(this, type, systemClassLoaderId)

                is InterfaceType ->
                    TypeInfo.ReferenceTypeInfo.CreatedType.ClassOrInterface.Interface(this, type, systemClassLoaderId)

                is ArrayType -> {
                    val componentTypeInfo = try {
                        getTypeInfo( type.componentType() )
                    } catch (e: ClassNotLoadedException) {
                        getNotYetLoadedTypeInfo( type.componentTypeName() )
                    }

                    TypeInfo.ReferenceTypeInfo.CreatedType.ArrayType(this, type, componentTypeInfo)
                }

                else -> throw RuntimeException("Encountered reference type which is neither a class, interface, nor array type. This is impossible by the JDI specification.")
            }

            is VoidType -> TypeInfo.VoidTypeInfo(this, type)
            else -> throw RuntimeException("Encountered type that is neither a primitive type, a reference type, nor void. This is impossible by the JDI specification.")
        }

        jdiTypeToTypeInfo[type] = typeInfo
        return typeInfo
    }

    fun getTypeInfo(type: ArrayType): TypeInfo.ReferenceTypeInfo.CreatedType.ArrayType =
        getTypeInfo(type as Type) as TypeInfo.ReferenceTypeInfo.CreatedType.ArrayType

    fun getTypeInfo(type: ReferenceType): TypeInfo.ReferenceTypeInfo.CreatedType =
        getTypeInfo(type as Type) as TypeInfo.ReferenceTypeInfo.CreatedType

    fun getNotYetLoadedTypeInfo(typeName: String): TypeInfo.ReferenceTypeInfo.NotYetLoadedType =
        TypeInfo.ReferenceTypeInfo.NotYetLoadedType(this, typeName)
}