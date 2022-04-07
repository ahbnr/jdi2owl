package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.runtime_components

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.TypeInfo
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure.withRefTypeContext
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.utils.addReferenceOrNullClass
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TripleCollector
import org.apache.jena.graph.NodeFactory

/**
 * This is actually more part of the static structure.
 * So it really should belong into ClassMapper.
 *
 * However, two arguments to put it here:
 *
 * * to avoid unnecessary triples, we should only generate them if there actually
 *   is a non-empty sequence being mapped
 * * for Iterables, due to type-erasure, we can only generate them if we can extract the
 *   concrete component type from the first element of a mapped, non-empty sequence
 *
 * But this also means, we dont get this static information if there are only empty arrays
 * // TODO Evaluate the effects of this decision
 */
fun mapSequenceTyping(context: SequenceContext) {
    with(context) {
        // TODO: Fix the problem of missing component types and remove the !! annotations here
        if (mappedSequenceComponentTypes.contains(context.componentTypeInfo!!.rcn))
            return

        mappedSequenceComponentTypes.add(context.componentTypeInfo!!.rcn)

        val typedSequenceElementURI = IRIs.prog.genTypedSequenceElementIRI(componentTypeInfo!!)

        // hasElement<type> Relation
        val typedHasElementURI = IRIs.prog.genTypedHasElementIRI(componentTypeInfo!!)

        tripleCollector.addStatement(
            typedHasElementURI,
            IRIs.rdf.type,
            IRIs.owl.ObjectProperty
        )

        tripleCollector.addStatement(
            typedHasElementURI,
            IRIs.rdf.type,
            IRIs.owl.InverseFunctionalProperty
        )

        tripleCollector.addStatement(
            typedHasElementURI,
            IRIs.rdfs.subPropertyOf,
            IRIs.java.hasElement
        )

        // Removed this part:
        //   We would have to declare the domain as the union of the array type and all possible
        //   iterable implementors.
        //   This is complex and does not have much use, so we dont declare the domain
        // val containerTypeUri = IRIs.prog.genReferenceTypeURI(containerType)
        // tripleCollector.addStatement(
        //     typedHasElementURI,
        //     IRIs.rdfs.domain,
        //     containerTypeUri
        // )

        tripleCollector.addStatement(
            typedHasElementURI,
            IRIs.rdfs.range,
            typedSequenceElementURI
        )

        tripleCollector.addStatement(
            typedSequenceElementURI,
            IRIs.rdf.type,
            IRIs.owl.Class
        )

        when (val componentTypeInfo = componentTypeInfo) {
            is TypeInfo.PrimitiveTypeInfo -> {
                tripleCollector.addStatement(
                    typedSequenceElementURI,
                    IRIs.rdfs.subClassOf,
                    IRIs.java.PrimitiveSequenceElement
                )

                // storesPrimitive Relation
                val typedStoresPrimitiveURI =
                    IRIs.prog.genTypedStoresPrimitiveIRI(componentTypeInfo)

                tripleCollector.addStatement(
                    typedStoresPrimitiveURI,
                    IRIs.rdf.type,
                    IRIs.owl.DatatypeProperty
                )

                tripleCollector.addStatement(
                    typedStoresPrimitiveURI,
                    IRIs.rdf.type,
                    IRIs.owl.FunctionalProperty
                )

                tripleCollector.addStatement(
                    typedStoresPrimitiveURI,
                    IRIs.rdfs.subPropertyOf,
                    IRIs.java.storesPrimitive
                )

                tripleCollector.addStatement(
                    typedStoresPrimitiveURI,
                    IRIs.rdfs.domain,
                    typedSequenceElementURI
                )

                val datatypeURI = IRIs.java.genPrimitiveTypeURI(componentTypeInfo)
                if (datatypeURI == null) {
                    logger.error("Unknown primitive data type: ${componentTypeInfo.rcn}.")
                    return
                }
                tripleCollector.addStatement(
                    typedStoresPrimitiveURI,
                    IRIs.rdfs.range,
                    datatypeURI
                )

                tripleCollector.addStatement(
                    typedSequenceElementURI,
                    IRIs.rdfs.subClassOf,
                    tripleCollector.addConstruct(
                        TripleCollector.BlankNodeConstruct.OWLSome(
                            typedStoresPrimitiveURI, NodeFactory.createURI(datatypeURI)
                        )
                    )
                )
            }
            is TypeInfo.ReferenceTypeInfo -> {
                tripleCollector.addStatement(
                    typedSequenceElementURI,
                    IRIs.rdfs.subClassOf,
                    IRIs.java.`SequenceElement%3CObject%3E`
                )

                // storesReference Relation
                val typedStoresReferenceURI =
                    IRIs.prog.genTypedStoresReferenceIRI(componentTypeInfo)

                tripleCollector.addStatement(
                    typedStoresReferenceURI,
                    IRIs.rdf.type,
                    IRIs.owl.ObjectProperty
                )

                tripleCollector.addStatement(
                    typedStoresReferenceURI,
                    IRIs.rdf.type,
                    IRIs.owl.FunctionalProperty
                )

                tripleCollector.addStatement(
                    typedStoresReferenceURI,
                    IRIs.rdfs.subPropertyOf,
                    IRIs.java.storesReference
                )

                tripleCollector.addStatement(
                    typedStoresReferenceURI,
                    IRIs.rdfs.domain,
                    typedSequenceElementURI
                )

                val componentTypeIRI = IRIs.prog.genReferenceTypeIRI(componentTypeInfo)
                tripleCollector.addStatement(
                    typedStoresReferenceURI,
                    IRIs.rdfs.range,
                    withRefTypeContext(componentTypeInfo, componentTypeIRI) {
                        addReferenceOrNullClass(this)
                    }
                )

                tripleCollector.addStatement(
                    typedSequenceElementURI,
                    IRIs.rdfs.subClassOf,
                    tripleCollector.addConstruct(
                        TripleCollector.BlankNodeConstruct.OWLSome(
                            typedStoresReferenceURI,
                            withRefTypeContext(componentTypeInfo, componentTypeIRI) {
                                addReferenceOrNullClass(this)
                            }
                        )
                    )
                )
            }
            else -> {
                logger.error("Encountered unknown array component type.")
                return
            }
        }
    }
}