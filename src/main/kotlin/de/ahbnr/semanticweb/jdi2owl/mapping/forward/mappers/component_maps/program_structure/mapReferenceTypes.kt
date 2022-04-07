package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.TypeInfo
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.contexts.MappingContext

fun mapReferenceTypes(context: MappingContext) =
    context.apply {
        val allReferenceTypes = buildParameters.jvmState.pausedThread.virtualMachine().allClasses()

        for (referenceType in allReferenceTypes) {
            val typeInfo = buildParameters.typeInfoProvider.getTypeInfo(referenceType)
            val typeIRI = IRIs.prog.genReferenceTypeURI(typeInfo)

            withCreatedTypeContext(typeInfo, typeIRI) {
                when (typeInfo) {
                    is TypeInfo.ReferenceTypeInfo.CreatedType.ClassOrInterface.Class ->
                        withClassContext(typeInfo) { mapClass(this) }

                    is TypeInfo.ReferenceTypeInfo.CreatedType.ArrayType ->
                        withArrayTypeContext(typeInfo) { mapArrayType(this) }

                    is TypeInfo.ReferenceTypeInfo.CreatedType.ClassOrInterface.Interface ->
                        withInterfaceContext(typeInfo) { mapInterface(this) }
                }
            }
        }
    }

interface RefTypeContext: MappingContext {
    val typeInfo: TypeInfo.ReferenceTypeInfo
    val typeIRI: String
}

interface CreatedTypeContext: RefTypeContext {
    override val typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType
    override val typeIRI: String
}

fun <R> MappingContext.withRefTypeContext(
    typeInfo: TypeInfo.ReferenceTypeInfo,
    typeIRI: String,
    block: RefTypeContext.() -> R
): R =
    object: MappingContext by this, RefTypeContext {
        override val typeInfo: TypeInfo.ReferenceTypeInfo = typeInfo
        override val typeIRI: String = typeIRI
    }.let(block)

fun <R> MappingContext.withCreatedTypeContext(
    typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType,
    typeIRI: String,
    block: CreatedTypeContext.() -> R
): R =
    object: MappingContext by this, CreatedTypeContext {
        override val typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType = typeInfo
        override val typeIRI: String = typeIRI
    }.let(block)