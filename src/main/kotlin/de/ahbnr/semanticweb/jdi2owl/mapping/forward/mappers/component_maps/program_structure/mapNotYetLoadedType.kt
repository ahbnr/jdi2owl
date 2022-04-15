package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.TypeInfo
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.contexts.MappingContext

/**
 * Some classes might not have been loaded by the JVM yet and are only known by name until now.
 * We reflect these incomplete types in the knowledge graph by typing them with java:UnloadedType
 *
 * See also https://docs.oracle.com/en/java/javase/11/docs/api/jdk.jdi/com/sun/jdi/ClassNotLoadedException.html
 */
fun mapNotYetLoadedType(context: NotYetLoadedTypeContext): Unit = with(context) {
    if (buildParameters.limiter.canReferenceTypeBeSkipped(typeInfo.binaryName))
        return

    // TODO: Check if we already added a triple for this unloaded type
    tripleCollector.addStatement(
        typeIRI,
        IRIs.rdf.type,
        IRIs.java.UnloadedType
    )

    // it is also an owl class
    // TODO: Why? Check model
    tripleCollector.addStatement(
        typeIRI,
        IRIs.rdf.type,
        IRIs.owl.Class
    )

    // all unloaded types must be reference types
    // and thus inherit from java.lang.Object
    tripleCollector.addStatement(
        typeIRI,
        IRIs.rdfs.subClassOf,
        IRIs.prog.java_lang_Object
    )
}

interface NotYetLoadedTypeContext: RefTypeContext {
    override val typeInfo: TypeInfo.ReferenceTypeInfo.NotYetLoadedType
    override val typeIRI: String
}

fun <R> MappingContext.withNotYetLoadedTypeContext(
    typeInfo: TypeInfo.ReferenceTypeInfo.NotYetLoadedType,
    typeIRI: String,
    block: NotYetLoadedTypeContext.() -> R
): R =
    object: MappingContext by this, NotYetLoadedTypeContext {
        override val typeInfo: TypeInfo.ReferenceTypeInfo.NotYetLoadedType = typeInfo
        override val typeIRI: String = typeIRI
    }.let(block)
