package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.program_structure

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TypeInfo

fun mapArrayType(context: ArrayTypeContext): Unit = with(context) {
    tripleCollector.addStatement(
        // this, as an individual, is an array:
        typeIRI, IRIs.rdf.type, IRIs.java.ArrayType
    )

    when (val componentType = typeInfo.componentType) {
        is TypeInfo.ReferenceTypeInfo -> {
            // Technically, we do not need to explicitly specify for every array type whose component type is a reference
            // type that it is subsumed by Object[],
            // because this can be transitively inferred from the supertypes.
            //
            // Still, it is useful to do so for query engines that have no inferencing capabilities (e.g. plain SPARQL)
            if (componentType.binaryName != "java.lang.Object") {
                tripleCollector.addStatement(
                    typeIRI,
                    IRIs.rdfs.subClassOf,
                    IRIs.prog.`java_lang_Object%5B%5D`
                )
            }

            if (componentType is TypeInfo.ReferenceTypeInfo.UnpreparedType) {
                val componentTypeInfo = buildParameters.typeInfoProvider.getNotYetLoadedTypeInfo(componentType.binaryName)
                val componentTypeIRI = IRIs.prog.genReferenceTypeIRI(componentTypeInfo)
                withUnpreparedTypeContext(componentTypeInfo, componentTypeIRI) {
                    mapUnpreparedType(this)
                }

                tripleCollector.addStatement(
                    typeIRI,
                    IRIs.rdfs.subClassOf,
                    IRIs.java.UnpreparedTypeArray
                )
            }
        }
        is TypeInfo.PrimitiveTypeInfo ->
            tripleCollector.addStatement(
                typeIRI,
                IRIs.rdfs.subClassOf,
                IRIs.java.PrimitiveArray
            )
        else -> logger.error("Encountered unknown kind of component type: ${typeInfo.componentType}")
    }

    pluginListeners.mapInContext(context)
}

interface ArrayTypeContext: CreatedTypeContext {
    override val typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType.ArrayType
}

fun CreatedTypeContext.withArrayTypeContext(
    typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType.ArrayType,
    block: ArrayTypeContext.() -> Unit
) {
    object: CreatedTypeContext by this, ArrayTypeContext {
        override val typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType.ArrayType = typeInfo
    }.apply(block)
}
