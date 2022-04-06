package de.ahbnr.semanticweb.jdi2owl.mapping.forward

import com.sun.jdi.ArrayType
import com.sun.jdi.ClassNotLoadedException
import com.sun.jdi.ClassType
import com.sun.jdi.InterfaceType
import com.sun.jdi.PrimitiveType
import com.sun.jdi.ReferenceType
import com.sun.jdi.Type
import com.sun.jdi.VirtualMachine
import com.sun.jdi.VoidType
import de.ahbnr.semanticweb.jdi2owl.Logger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

sealed class EnclosedTypeKind {
    object Unknown: EnclosedTypeKind()
    class NamedMemberClass(val simpleName: String): EnclosedTypeKind()
    class AnonymousClass(val id: Long): EnclosedTypeKind()
    class LocalClass(val simpleName: String, val id: Long): EnclosedTypeKind()
}

data class SignatureInfo(
    val lastNameSegment: String,
    val enclosedTypeKind: EnclosedTypeKind
) {
    companion object {
        private val memberClassNameRegex = Regex("^[^\\$]+\\$([^\\d].*)$")
        private val anonymousClassNameRegex = Regex("^[^\\$]+\\$(\\d+)$")
        private val localClassNameRegex = Regex("^[^\\$]+\\$(\\d+)(.+)$")

        fun fromJdiTypeName(typeName: String): SignatureInfo {
            val lastNameSegment = typeName.split('.').last()
            val enclosedTypeKind =
                memberClassNameRegex
                    .matchEntire(lastNameSegment)
                    ?.let {
                        val simpleName = it.groupValues[1]
                        EnclosedTypeKind.NamedMemberClass(simpleName)
                    }
                ?: anonymousClassNameRegex
                    .matchEntire(lastNameSegment)
                    ?.let {
                        val id = it.groupValues[1].toLong()
                        EnclosedTypeKind.AnonymousClass(id)
                    }
                ?: localClassNameRegex
                    .matchEntire(lastNameSegment)
                    ?.let {
                        val (idRaw, simpleName) = it.groupValues.drop(1)
                        EnclosedTypeKind.LocalClass(simpleName, idRaw.toLong())
                    }
                ?: EnclosedTypeKind.Unknown

            return SignatureInfo(
                lastNameSegment = lastNameSegment,
                enclosedTypeKind = enclosedTypeKind
            )
        }

        fun fromJdiType(jdiType: Type): SignatureInfo =
            fromJdiTypeName(jdiType.name())
    }
}

private object RCNBuildingErrorHandler: KoinComponent {
    private val logger: Logger by inject()

    fun handleError(message: String, fallback: String): String {
        logger.error("Could not derive RCN of a JDI state component.")
        logger.log("Reason: $message")
        logger.log("Falling back to JDI generated name.")

        return fallback
    }
}

sealed class TypeInfo {
    abstract val rcn: String

    class VoidTypeInfo(val type: VoidType): TypeInfo() {
        override val rcn: String = "void"
    }

    class PrimitiveTypeInfo(val type: PrimitiveType): TypeInfo() {
        override val rcn: String = type.name()
    }

    sealed class ReferenceTypeInfo: TypeInfo() {
        sealed class CreatedType(
            open val type: ReferenceType
        ): ReferenceTypeInfo() {
            class TopLevelClassOrInterface(type: ReferenceType): CreatedType(type) {
                override val rcn: String = type.name()
            }

            class ArrayType(override val type: com.sun.jdi.ArrayType, componentType: TypeInfo): CreatedType(type) {
                override val rcn: String = "${componentType.rcn}[]"
            }

            sealed class EnclosedType(
                type: ReferenceType,
                val surroundingType: ReferenceTypeInfo
            ): CreatedType(type) {
                class MemberClassOrInterface(
                    type: ReferenceType,
                    surroundingType: ReferenceTypeInfo,
                    signatureInfo: SignatureInfo
                ): EnclosedType(type, surroundingType) {
                    override val rcn: String
                    init {
                        rcn = if (signatureInfo.enclosedTypeKind !is EnclosedTypeKind.NamedMemberClass)
                            RCNBuildingErrorHandler.handleError(
                                "Binary name of member class ${type.name()} does not follow naming scheme.",
                                type.name()
                            )

                        else "${surroundingType.rcn}.${signatureInfo.enclosedTypeKind.simpleName}"
                    }
                }

                class AnonymousClass(
                    override val type: ClassType,
                    surroundingType: ReferenceTypeInfo,
                    signatureInfo: SignatureInfo
                ): EnclosedType(type, surroundingType) {
                    override val rcn: String
                    init {
                        rcn = if (signatureInfo.enclosedTypeKind !is EnclosedTypeKind.AnonymousClass)
                            RCNBuildingErrorHandler.handleError(
                                "Binary name of anonymous class ${type.name()} does not follow naming scheme.",
                                type.name()
                            )

                        else "${surroundingType.rcn}.:Anon:${signatureInfo.enclosedTypeKind.id}"
                    }
                }

                class LocalClass(
                    override val type: ClassType,
                    surroundingType: ReferenceTypeInfo,
                    signatureInfo: SignatureInfo
                ): EnclosedType(type, surroundingType) {
                    private companion object {
                        val nameRegex = Regex("\\$(\\d+)(.+)")
                    }

                    override val rcn: String
                    init {
                        rcn = if (signatureInfo.enclosedTypeKind !is EnclosedTypeKind.LocalClass)
                                RCNBuildingErrorHandler.handleError("Name of local class does not follow naming scheme.", type.name())
                            else
                                "${surroundingType.rcn}.${signatureInfo.enclosedTypeKind.simpleName}:Local:${signatureInfo.enclosedTypeKind.id}"
                    }
                }

                class LambdaExpressionClass(
                    override val type: ClassType,
                    surroundingType: ReferenceTypeInfo
                ): EnclosedType(type, surroundingType) {
                    override val rcn: String = type.name()
                }
            }
        }

        class NotYetLoadedType(val binaryName: String): ReferenceTypeInfo() {
            override val rcn: String = "NotYetLoaded:$binaryName"
        }
    }
}

class TypeInfoProvider(vm: VirtualMachine) {
    private val jdiTypeToTypeInfo = mutableMapOf<Type, TypeInfo>()
    private val nestedTypeToSurroundingType: Map<ReferenceType, ReferenceType>

    init {
        val allReferenceTypes = vm.allClasses()

        // Pass 1: Create a reverse map from nested types to their surrounding types
        val nestedTypeToSurroundingType: MutableMap<ReferenceType, ReferenceType> = mutableMapOf()
        for (refType in allReferenceTypes) {
            for (nestedType in refType.nestedTypes()) {
                nestedTypeToSurroundingType[nestedType] = refType
            }
        }

        this.nestedTypeToSurroundingType = nestedTypeToSurroundingType
    }

    fun getTypeInfo(type: Type): TypeInfo {
        jdiTypeToTypeInfo[type]?.let {
            return it
        }

        val typeInfo = when (type) {
            is PrimitiveType -> TypeInfo.PrimitiveTypeInfo(type)
            is ReferenceType -> when (type) {
                is InterfaceType, is ClassType -> {
                    val surroundingType = nestedTypeToSurroundingType[type]
                    if (surroundingType == null) // Top-level type!
                        TypeInfo.ReferenceTypeInfo.CreatedType.TopLevelClassOrInterface(type)

                    else {
                        val surroundingTypeInfo = getTypeInfo(surroundingType) as TypeInfo.ReferenceTypeInfo

                        val signatureInfo = SignatureInfo.fromJdiType(type)

                        when (type) {
                            is InterfaceType ->
                                TypeInfo.ReferenceTypeInfo.CreatedType.EnclosedType.MemberClassOrInterface(type, surroundingTypeInfo, signatureInfo)

                            is ClassType -> {
                                when (signatureInfo.enclosedTypeKind) {
                                    is EnclosedTypeKind.NamedMemberClass ->
                                        TypeInfo.ReferenceTypeInfo.CreatedType.EnclosedType.MemberClassOrInterface(
                                            type, surroundingTypeInfo, signatureInfo
                                        )

                                    is EnclosedTypeKind.AnonymousClass ->
                                        TypeInfo.ReferenceTypeInfo.CreatedType.EnclosedType.AnonymousClass(
                                            type, surroundingTypeInfo, signatureInfo
                                        )

                                    is EnclosedTypeKind.LocalClass ->
                                        TypeInfo.ReferenceTypeInfo.CreatedType.EnclosedType.LocalClass(
                                            type, surroundingTypeInfo, signatureInfo
                                        )

                                    is EnclosedTypeKind.Unknown ->
                                        TypeInfo.ReferenceTypeInfo.CreatedType.EnclosedType.LambdaExpressionClass(
                                            type, surroundingTypeInfo
                                        )
                                }
                            }

                            else -> throw RuntimeException("Encountered nested type which is neither a class or interface. This is not allowed by the JLS and JVMS.")
                        }
                    }
                }

                is ArrayType -> {
                    val componentTypeInfo = try {
                        getTypeInfo( type.componentType() )
                    } catch (e: ClassNotLoadedException) {
                        getNotYetLoadedTypeInfo( type.componentTypeName() )
                    }

                    TypeInfo.ReferenceTypeInfo.CreatedType.ArrayType(type, componentTypeInfo)
                }

                else -> throw RuntimeException("Encountered reference type which is neither a class, interface, nor array type. This is impossible by the JDI specification.")
            }

            is VoidType -> TypeInfo.VoidTypeInfo(type)
            else -> throw RuntimeException("Encountered type that is neither a primitive type, a reference type, nor void. This is impossible by the JDI specification.")
        }

        jdiTypeToTypeInfo[type] = typeInfo
        return typeInfo
    }

    fun getTypeInfo(type: ReferenceType): TypeInfo.ReferenceTypeInfo =
        getTypeInfo(type as Type) as TypeInfo.ReferenceTypeInfo

    fun getNotYetLoadedTypeInfo(typeName: String): TypeInfo.ReferenceTypeInfo.NotYetLoadedType =
        TypeInfo.ReferenceTypeInfo.NotYetLoadedType(typeName)
}