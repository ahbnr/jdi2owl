package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure

import com.sun.jdi.ClassNotLoadedException
import com.sun.jdi.PrimitiveType
import com.sun.jdi.ReferenceType
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.TypeInfo
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.JavaType

fun mapArrayType(context: ArrayTypeContext): Unit = with(context) {
    tripleCollector.addStatement(
        // this, as an individual, is an array:
        typeIRI, IRIs.rdf.type, IRIs.java.Array
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

            if (componentType is TypeInfo.ReferenceTypeInfo.NotYetLoadedType) {
                val componentTypeInfo = buildParameters.typeInfoProvider.getNotYetLoadedTypeInfo(componentType.binaryName)
                val componentTypeIRI = IRIs.prog.genReferenceTypeIRI(componentTypeInfo)
                withNotYetLoadedTypeContext(componentTypeInfo, componentTypeIRI) {
                    mapNotYetLoadedType(this)
                }

                tripleCollector.addStatement(
                    typeIRI,
                    IRIs.rdfs.subClassOf,
                    IRIs.java.UnloadedTypeArray
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
