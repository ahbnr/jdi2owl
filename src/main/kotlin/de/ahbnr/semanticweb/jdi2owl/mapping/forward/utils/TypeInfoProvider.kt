package de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils

import com.sun.jdi.*
import de.ahbnr.semanticweb.jdi2owl.debugging.mirrors.ClassLoaderMirror

class TypeInfoProvider(mainThread: ThreadReference) {
    private val jdiTypeToTypeInfo = mutableMapOf<Type, TypeInfo>()
    private val systemClassLoaderId: Long? = ClassLoaderMirror.Static.getBuiltInClassLoaderType(mainThread)
        ?.getSystemClassLoader()
        ?.uniqueID()

    val vm = mainThread.virtualMachine()

    val java_lang_Object: TypeInfo.ReferenceTypeInfo.CreatedType.ClassOrInterface.Class
    val `java_lang_Object%5B%5D`: TypeInfo.ReferenceTypeInfo
    val java_lang_Cloneable: TypeInfo.ReferenceTypeInfo
    val java_io_Serializable: TypeInfo.ReferenceTypeInfo

    init {
        fun getReferenceTypeInfo(binaryName: String) =
            getPreparedTypeInfoByName(binaryName, null)
                ?: getNotYetLoadedTypeInfo(binaryName)

        java_lang_Object = getPreparedTypeInfoByName("java.lang.Object", null)
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
     */
    fun getPreparedTypeInfoByName(binaryName: String, classLoader: ClassLoaderReference?): TypeInfo.ReferenceTypeInfo.CreatedType? =
        vm
            .classesByName(binaryName)
            .firstOrNull {
                it.classLoader() == classLoader
            }
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

    fun getNotYetLoadedTypeInfo(typeName: String): TypeInfo.ReferenceTypeInfo.UnpreparedType =
        TypeInfo.ReferenceTypeInfo.UnpreparedType(this, typeName)
}