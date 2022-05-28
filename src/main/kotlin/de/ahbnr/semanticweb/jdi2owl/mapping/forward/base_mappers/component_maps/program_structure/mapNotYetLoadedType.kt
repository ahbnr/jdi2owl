package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.program_structure

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.MappingContext
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.mapJavaNameToLiteral
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TypeInfo

/**
 * Some classes might not have been prepared by the JVM yet and are only known by name until now.
 * We reflect these incomplete types in the knowledge graph by typing them with java:UnloadedType
 */
fun mapUnpreparedType(context: UnpreparedTypeContext): Unit = with(context) {
    if (buildParameters.limiter.canReferenceTypeBeSkipped(typeInfo.binaryName))
        return

    with(IRIs) {
        tripleCollector.dsl {
            typeIRI {
                rdf.type of owl.NamedIndividual
                rdf.type of java.UnloadedType
                java.hasName of mapJavaNameToLiteral(typeInfo.binaryName)

                // all unloaded types must be reference types
                // and thus inherit from java.lang.Object
                rdf.type of owl.Class
                rdfs.subClassOf of prog.java_lang_Object
                // however, they have no instances, so we also declare them as being subsumed by nothing.
                rdfs.subClassOf of owl.Nothing
            }
        }
    }

    pluginListeners.mapInContext(context)
}

interface UnpreparedTypeContext: RefTypeContext {
    override val typeInfo: TypeInfo.ReferenceTypeInfo.UnpreparedType
    override val typeIRI: String
}

fun <R> MappingContext.withUnpreparedTypeContext(
    typeInfo: TypeInfo.ReferenceTypeInfo.UnpreparedType,
    typeIRI: String,
    block: UnpreparedTypeContext.() -> R
): R =
    object: MappingContext by this, UnpreparedTypeContext {
        override val typeInfo: TypeInfo.ReferenceTypeInfo.UnpreparedType = typeInfo
        override val typeIRI: String = typeIRI
    }.let(block)
