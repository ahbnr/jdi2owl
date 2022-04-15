package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.runtime_components

import com.sun.jdi.ObjectReference
import de.ahbnr.semanticweb.jdi2owl.debugging.JvmObjectIterator
import de.ahbnr.semanticweb.jdi2owl.debugging.ReferenceContexts
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.contexts.MappingContext
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.ValueToNodeMapper

fun mapObjects(context: MappingContext) =
    with(context) {
        val referenceContexts = ReferenceContexts()
        val valueMapper = ValueToNodeMapper()

        val iterator = JvmObjectIterator(
            buildParameters,
            referenceContexts
        )
        val allObjects = iterator.iterateObjects().toList()
        iterator.reportErrors()

        // Names of those component types of arrays and iterables for which typed sequence element triples
        // already have been added
        val mappedSequenceComponentTypes = mutableSetOf<String>()

        withObjectMappingContext(
            allObjects,
            referenceContexts,
            valueMapper,
            mappedSequenceComponentTypes
        ) {
            for (objectReference in allObjects) {
                val objectIRI = IRIs.run.genObjectIRI(objectReference)

                val type = objectReference.referenceType()
                val typeInfo = buildParameters.typeInfoProvider.getTypeInfo(type)
                val typeIRI = IRIs.prog.genReferenceTypeIRI(typeInfo)

                withObjectContext(
                    `object` = objectReference,
                    objectIRI = objectIRI,
                    typeInfo = typeInfo,
                    typeIRI = typeIRI
                ) {
                    mapObject(this)
                }
            }

            mapTypeInstanceClosure(this)

            mapStaticClassMembers(this)
        }
    }

interface ObjectMappingContext: MappingContext {
    val allObjects: List<ObjectReference>
    val referenceContexts: ReferenceContexts
    val valueMapper: ValueToNodeMapper
    val mappedSequenceComponentTypes: MutableSet<String>
}

fun <R> MappingContext.withObjectMappingContext(
    allObjects: List<ObjectReference>,
    referenceContexts: ReferenceContexts,
    valueMapper: ValueToNodeMapper,
    mappedSequenceComponentTypes: MutableSet<String>,
    block: ObjectMappingContext.() -> R
): R =
    object: MappingContext by this, ObjectMappingContext {
        override val allObjects: List<ObjectReference> = allObjects
        override val referenceContexts: ReferenceContexts = referenceContexts
        override val valueMapper: ValueToNodeMapper = valueMapper
        override val mappedSequenceComponentTypes: MutableSet<String> = mappedSequenceComponentTypes
    }.let(block)
