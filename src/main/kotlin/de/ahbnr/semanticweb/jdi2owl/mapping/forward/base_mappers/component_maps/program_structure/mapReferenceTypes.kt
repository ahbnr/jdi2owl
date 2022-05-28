package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.program_structure

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.MappingContext
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.mapJavaNameToLiteral
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TypeInfo

fun mapReferenceTypes(context: MappingContext) = with(context) {
    val allReferenceTypes = buildParameters.jvmState.pausedThread.virtualMachine().allClasses()

    for (referenceType in allReferenceTypes) {
        val typeInfo = buildParameters.typeInfoProvider.getTypeInfo(referenceType)
        val typeIRI = IRIs.prog.genReferenceTypeIRI(typeInfo)

        withCreatedTypeContext(typeInfo, typeIRI) {
            mapReferenceType(this)
        }
    }
}

fun mapReferenceType(context: CreatedTypeContext) = with(context) {
    if (buildParameters.limiter.canReferenceTypeBeSkipped(typeInfo.jdiType))
        return

    with(IRIs) {
        tripleCollector.dsl {
            typeIRI {
                // `typeIRI` is a concept
                rdf.type of owl.Class
                // and an individual (punning)
                rdf.type of owl.NamedIndividual
                // and it has a name
                java.hasName of mapJavaNameToLiteral(typeInfo.jdiType.name())
            }
        }
    }

    mapFields(this)
    mapMethods(this)

    for (directSupertypeInfo in typeInfo.getDirectSupertypes()) {
        if (buildParameters.limiter.canReferenceTypeBeSkipped(directSupertypeInfo))
            continue

        val directSupertypeIRI = IRIs.prog.genReferenceTypeIRI(directSupertypeInfo)

        tripleCollector.addStatement(
            typeIRI,
            IRIs.rdfs.subClassOf,
            directSupertypeIRI
        )

        if (directSupertypeInfo is TypeInfo.ReferenceTypeInfo.UnpreparedType) {
            withUnpreparedTypeContext(directSupertypeInfo, directSupertypeIRI) {
                mapUnpreparedType(this)
            }
        }
    }

    // Technically, we do not need to explicitly specify for every reference type that it is subsumed by Object,
    // because this can be transitively inferred from the supertypes.
    //
    // Still, it is useful to do so for query engines that have no inferencing capabilities (e.g. plain SPARQL)
    // Also, this way, we retain Object as a supertype even if the chain of supertypes is interrupted by the mapping
    // limiter
    if (typeInfo.binaryName != "java.lang.Object") {
        tripleCollector.addStatement(
            typeIRI,
            IRIs.rdfs.subClassOf,
            IRIs.prog.java_lang_Object
        )
    }

    when (val typeInfo = typeInfo) {
        is TypeInfo.ReferenceTypeInfo.CreatedType.ClassOrInterface.Class ->
            withClassContext(typeInfo) { mapClass(this) }

        is TypeInfo.ReferenceTypeInfo.CreatedType.ClassOrInterface.Interface ->
            withInterfaceContext(typeInfo) { mapInterface(this) }

        is TypeInfo.ReferenceTypeInfo.CreatedType.ArrayType ->
            withArrayTypeContext(typeInfo) { mapArrayType(this) }
    }

    pluginListeners.mapInContext(this)
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
