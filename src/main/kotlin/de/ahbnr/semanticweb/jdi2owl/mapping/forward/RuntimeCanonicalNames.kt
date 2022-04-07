package de.ahbnr.semanticweb.jdi2owl.mapping.forward

import com.sun.jdi.ArrayType
import com.sun.jdi.ClassNotLoadedException
import com.sun.jdi.ClassType
import com.sun.jdi.Field
import com.sun.jdi.InterfaceType
import com.sun.jdi.LocalVariable
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

private object RCNBuildingErrorHandler: KoinComponent {
    private val logger: Logger by inject()

    fun handleError(message: String, fallback: String): String {
        logger.error("Could not derive RCN of a JDI state component.")
        logger.log("Reason: $message")
        logger.log("Falling back to JDI generated name.")

        return fallback
    }
}

interface HasRCN {
    val rcn: String
}

class FieldInfo(typeInfo: TypeInfo.ReferenceTypeInfo, val jdiField: Field): HasRCN {
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
        sealed class CreatedType(
            typeInfoProvider: TypeInfoProvider,
            open val jdiType: ReferenceType
        ): ReferenceTypeInfo(typeInfoProvider) {
            fun getFieldInfo(field: Field) = FieldInfo(this, field)
            fun getMethodInfo(jdiMethod: Method) = MethodInfo(typeInfoProvider, jdiMethod)

            sealed class ClassOrInterface: CreatedType {
                override val jdiType: ReferenceType
                override val rcn: String

                constructor(
                    typeInfoProvider: TypeInfoProvider,
                    jdiType: ReferenceType,
                    systemClassLoaderId: Long?
                ): super(typeInfoProvider, jdiType) {
                    this.jdiType = jdiType

                    val classLoader = jdiType.classLoader()
                    val typeName = jdiType.name()

                    rcn =
                        if (classLoader == null)
                            typeName
                        else if (classLoader.uniqueID() == systemClassLoaderId)
                            "SysLoader~$typeName"
                        else
                            "Loader${classLoader.uniqueID()}~$typeName"
                }

                class Class(
                    typeInfoProvider: TypeInfoProvider,
                    override val jdiType: ClassType,
                    systemClassLoaderId: Long?
                ): ClassOrInterface(typeInfoProvider, jdiType, systemClassLoaderId)

                class Interface(
                    typeInfoProvider: TypeInfoProvider,
                    override val jdiType: InterfaceType,
                    systemClassLoaderId: Long?
                ): ClassOrInterface(typeInfoProvider, jdiType, systemClassLoaderId)
            }

            class ArrayType(
                typeInfoProvider: TypeInfoProvider,
                override val jdiType: com.sun.jdi.ArrayType,
                val componentType: TypeInfo
            ): CreatedType(typeInfoProvider, jdiType) {
                override val rcn: String = "${componentType.rcn}[]"
            }
        }

        class NotYetLoadedType(
            typeInfoProvider: TypeInfoProvider,
            val binaryName: String
        ): ReferenceTypeInfo(typeInfoProvider) {
            override val rcn: String = "NotYetLoaded~$binaryName"
        }
    }
}

class TypeInfoProvider(mainThread: ThreadReference) {
    private val jdiTypeToTypeInfo = mutableMapOf<Type, TypeInfo>()
    private val systemClassLoaderId: Long?

    init {
        systemClassLoaderId = ClassLoaderMirror.Static
            .getBuiltInClassLoaderType(mainThread)
            ?.getSystemClassLoader()
            ?.uniqueID()
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

    fun getTypeInfo(type: ReferenceType): TypeInfo.ReferenceTypeInfo.CreatedType =
        getTypeInfo(type as Type) as TypeInfo.ReferenceTypeInfo.CreatedType

    fun getNotYetLoadedTypeInfo(typeName: String): TypeInfo.ReferenceTypeInfo.NotYetLoadedType =
        TypeInfo.ReferenceTypeInfo.NotYetLoadedType(this, typeName)
}