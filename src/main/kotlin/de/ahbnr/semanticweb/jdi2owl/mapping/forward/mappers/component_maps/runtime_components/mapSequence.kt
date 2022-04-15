package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.runtime_components

import com.sun.jdi.Value
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TypeInfo
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TripleCollector
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.NodeFactory

fun mapSequence(context: SequenceContext) {
    with(context) {
        // Encode container size in cardinality restriction
        // (otherwise we won't be able to reliably query for arrays / iterables by their size due to the open world assumption)
        tripleCollector.addStatement(
            objectIRI,
            IRIs.rdf.type,
            tripleCollector.addConstruct(
                TripleCollector.BlankNodeConstruct.OWLObjectCardinalityRestriction(
                    onPropertyUri = IRIs.java.hasElement,
                    onClassUri = IRIs.java.SequenceElement,
                    cardinality = TripleCollector.BlankNodeConstruct.CardinalityType.Exactly(cardinality)
                )
            )
        )

        // The following code closes range set of hasElement for this container object
        //
        // Otherwise, we will not be able to do universal queries a la `hasElement only (storesReference only {null})`
        // Apparently reasoners (at least HermiT and Openllet) can not infer from the cardinality restriction
        //   above, that the elements of the array are restricted to those being explicitly listed.
        //
        // This might be because we have no unique name assumption.
        // I.e. the reasoner can not return a result for `hasElement only {elementX}` given the facts
        //   someArray hasElement elementX
        //   someArray a (hasElement exactly 1 SequenceElement)
        // because there might be some elementY such that
        //   someArray hasElement elementY
        // this does not violate the cardinality restriction, because it could be that
        //   elementY sameAs elementX
        //
        // Still, then `hasElement only {elementX}` can return someArray because
        // it is the same as asking `hasElment only {elementY}`.
        //
        // Maybe this kind of reasoning is just too complex and not implemented?
        //
        // As a workaround, we close the range of hasElement for this particular object explicitly:
        //
        //   inverse hasElement {myContainer} EquivalentTo { ...elements...}
        //
        // In turtle format:
        //   [
        //     a owl:Class ;
        //     owl:oneOf ( ..elements... ) ;
        //     owl:equivalentClass [
        //       a owl:Restriction ;
        //       owl:onProperty [
        //          a owl:ObjectProperty ;
        //          owl:inverseOf java:hasElement
        //       ] ;
        //       owl:someValuesFrom [
        //          a owl:Class ;
        //          owl:oneOf ( myContainer )
        //       ]
        //     ]
        //   ] .
        val elementIRIs = elements
            .indices
            .map { idx -> IRIs.run.genSequenceElementInstanceIRI(`object`, idx) }
        tripleCollector.addStatement(
            if (elementIRIs.isEmpty())
                NodeFactory.createURI(IRIs.owl.Nothing)
            else
                tripleCollector.addConstruct(
                    TripleCollector.BlankNodeConstruct.OWLOneOf.fromIRIs(elementIRIs)
                ),
            IRIs.owl.equivalentClass,
            tripleCollector.addConstruct(
                TripleCollector.BlankNodeConstruct.OWLSome(
                    // We can NOT use a named version of hasElement here.
                    // It does not work.
                    //  (Probably it will always be assumed to be different from the other inversions of hasElement)
                    tripleCollector.addConstruct(
                        TripleCollector.BlankNodeConstruct.OWLInverseProperty(IRIs.java.hasElement)
                    ),
                    tripleCollector.addConstruct(
                        TripleCollector.BlankNodeConstruct.OWLOneOf.fromIRIs(
                            listOf(objectIRI)
                        )
                    )
                )
            )
        )

        val componentTypeInfo = this.componentTypeInfo ?: return

        mapSequenceTyping(this)

        // # More concrete hasElement relation
        // Create sub-relation of hasElement<Type> relation for this particular array/iterable object to encode
        // the container size in the cardinalit == nully
        val typedHasElementIRI = IRIs.prog.genTypedHasElementIRI(componentTypeInfo)
        val typedSequenceElementIRI = IRIs.prog.genTypedSequenceElementIRI(componentTypeInfo)

        // add the actual elements
        for ((idx, elementValue) in elements.withIndex()) {
            val elementInstanceIRI = IRIs.run.genSequenceElementInstanceIRI(`object`, idx)
            tripleCollector.addStatement(
                elementInstanceIRI,
                IRIs.rdf.type,
                IRIs.owl.NamedIndividual
            )

            tripleCollector.addStatement(
                elementInstanceIRI,
                IRIs.rdf.type,
                typedSequenceElementIRI
            )

            tripleCollector.addStatement(
                elementInstanceIRI,
                IRIs.java.hasIndex,
                NodeFactory.createLiteralByValue(idx, XSDDatatype.XSDint)
            )

            if (idx < elements.size - 1) {
                val nextElementInstanceIRI = IRIs.run.genSequenceElementInstanceIRI(`object`, idx + 1)

                tripleCollector.addStatement(
                    elementInstanceIRI,
                    IRIs.java.hasSuccessor,
                    nextElementInstanceIRI
                )
            } else {
                // encode hasSuccessor cardinality, to make it clear that there is no successor
                // (we are closing the world here!)
                tripleCollector.addStatement(
                    elementInstanceIRI,
                    IRIs.rdf.type,
                    tripleCollector.addConstruct(
                        TripleCollector.BlankNodeConstruct.OWLObjectCardinalityRestriction(
                            onPropertyUri = IRIs.java.hasSuccessor,
                            onClassUri = IRIs.java.SequenceElement,
                            cardinality = TripleCollector.BlankNodeConstruct.CardinalityType.Exactly(0)
                        )
                    )
                )
            }

            tripleCollector.addStatement(
                objectIRI,
                typedHasElementIRI,
                elementInstanceIRI
            )

            val valueNode = valueMapper.map(elementValue)
            if (valueNode != null) {
                when (val componentTypeInfo = componentTypeInfo) {
                    is TypeInfo.PrimitiveTypeInfo -> {
                        val typedStoresPrimitiveIRI = IRIs.prog.genTypedStoresPrimitiveIRI(componentTypeInfo)
                        tripleCollector.addStatement(
                            elementInstanceIRI,
                            typedStoresPrimitiveIRI,
                            valueNode
                        )
                    }
                    is TypeInfo.ReferenceTypeInfo -> {
                        val typedStoresReferenceIRI = IRIs.prog.genTypedStoresReferenceIRI(componentTypeInfo)
                        tripleCollector.addStatement(
                            elementInstanceIRI,
                            typedStoresReferenceIRI,
                            valueNode
                        )
                    }
                    else -> {
                        logger.error("Encountered unknown component type: ${componentTypeInfo.rcn}")
                        return
                    }
                }
            }
        }
    }
}

interface SequenceContext: ObjectContext {
    val cardinality: Int
    val componentTypeInfo: TypeInfo?
    val elements: List<Value?>
}

fun ObjectContext.withSequenceContext(
    cardinality: Int,
    componentTypeInfo: TypeInfo?,
    elements: List<Value?>,
    block: SequenceContext.() -> Unit
) {
    object: ObjectContext by this, SequenceContext {
        override val cardinality: Int = cardinality
        override val componentTypeInfo: TypeInfo? = componentTypeInfo
        override val elements: List<Value?> = elements
    }.apply(block)
}
