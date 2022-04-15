package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.mapJavaNameToLiteral
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TypeInfo
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
            }
        }
    }
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
