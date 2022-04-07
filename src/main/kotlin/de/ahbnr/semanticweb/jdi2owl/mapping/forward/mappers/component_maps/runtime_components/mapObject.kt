package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.runtime_components

import com.sun.jdi.*
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.TypeInfo
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure.CreatedTypeContext
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.NodeFactory

fun mapObject(context: ObjectContext) = with(context) {
    // The object is a particular individual (not a class/concept)
    tripleCollector.addStatement(
        objectIRI,
        IRIs.rdf.type,
        IRIs.owl.NamedIndividual
    )

    // it is a java object
    tripleCollector.addStatement(
        objectIRI,
        IRIs.rdf.type,
        IRIs.java.Object
    )

    // as such, it has been assigned a unique ID by the VM JDWP agent:
    tripleCollector.addStatement(
        objectIRI,
        IRIs.java.hasJDWPObjectId,
        NodeFactory.createLiteralByValue(
            `object`.uniqueID(),
            XSDDatatype.XSDlong
        )
    )

    // it is of a particular java class
    tripleCollector.addStatement(
        objectIRI,
        IRIs.rdf.type,
        IRIs.prog.genReferenceTypeURI(typeInfo)
    )

    // TODO: This should also apply to non class types
    (typeInfo as? TypeInfo.ReferenceTypeInfo.CreatedType.ClassOrInterface.Class)?.let { typeInfo ->
        mapFields(this)

        mapIterable(this)

        mapPrimitiveWrapperObject(this)
    }

    // Try mapping it as an array, if it is an array
    mapArray(this)

    // Try mapping it as a string, if it is a string
    mapString(this)
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
