package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.objects

import com.sun.jdi.ObjectReference
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.program_structure.CreatedTypeContext
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TypeInfo
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.NodeFactory

fun mapObject(context: ObjectContext) = with(context) {
    with(IRIs) {
        tripleCollector.dsl {
            objectIRI {
                // The object is a particular individual (not a class/concept)
                rdf.type of owl.NamedIndividual
                // it is a java object
                rdf.type of java.Object
                // as such, it has been assigned a unique ID by the VM JPDA backend:
                java.hasUniqueId of NodeFactory.createLiteralByValue(
                    `object`.uniqueID(),
                    XSDDatatype.XSDlong
                )
                // it is of a particular java class
                rdf.type of prog.genReferenceTypeIRI(typeInfo)
            }
        }
    }

    mapFields(this)

    // Try mapping it as an instance of a primitive value mapper class, if it is such a class
    mapPrimitiveWrapperObject(this)

    // Try mapping it as a string, if it is a string
    mapString(this)

    // Try mapping it as an iterable, if it is an instance of a class implementing Iterable
    mapIterable(this)

    // Try mapping it as an array, if it is an array
    mapArray(this)

    pluginListeners.mapInContext(this)
}

interface ObjectContext: ObjectMappingContext, CreatedTypeContext {
    val `object`: ObjectReference
    val objectIRI: String
}

fun ObjectMappingContext.withObjectContext(
    `object`: ObjectReference,
    objectIRI: String,
    typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType,
    typeIRI: String,
    block: ObjectContext.() -> Unit
) {
    object: ObjectMappingContext by this, ObjectContext, CreatedTypeContext {
        override val `object`: ObjectReference = `object`
        override val objectIRI: String = objectIRI
        override val typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType = typeInfo
        override val typeIRI = typeIRI
    }.apply(block)
}
