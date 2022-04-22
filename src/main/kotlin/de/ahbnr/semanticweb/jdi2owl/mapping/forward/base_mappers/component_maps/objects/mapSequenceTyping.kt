package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.objects

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.program_structure.withRefTypeContext
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.utils.addReferenceOrNullClass
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TripleCollector
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TypeInfo
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

        val typedSequenceElementIRI = IRIs.prog.genTypedSequenceElementIRI(componentTypeInfo!!)

        // hasElement<type> Relation
        val typedHasElementIRI = IRIs.prog.genTypedHasElementIRI(componentTypeInfo!!)

        tripleCollector.addStatement(
            typedHasElementIRI,
            IRIs.rdf.type,
            IRIs.owl.ObjectProperty
        )

        tripleCollector.addStatement(
            typedHasElementIRI,
            IRIs.rdf.type,
            IRIs.owl.InverseFunctionalProperty
        )

        tripleCollector.addStatement(
            typedHasElementIRI,
            IRIs.rdfs.subPropertyOf,
            IRIs.java.hasElement
        )

        // Removed this part:
        //   We would have to declare the domain as the union of the array type and all possible
        //   iterable implementors.
        //   This is complex and does not have much use, so we dont declare the domain
        // val containerTypeUri = IRIs.prog.genReferenceTypeIRI(containerType)
        // tripleCollector.addStatement(
        //     typedHasElementIRI,
        //     IRIs.rdfs.domain,
        //     containerTypeUri
        // )

        tripleCollector.addStatement(
            typedHasElementIRI,
            IRIs.rdfs.range,
            typedSequenceElementIRI
        )

        tripleCollector.addStatement(
            typedSequenceElementIRI,
            IRIs.rdf.type,
            IRIs.owl.Class
        )

        when (val componentTypeInfo = componentTypeInfo) {
            is TypeInfo.PrimitiveTypeInfo -> {
                tripleCollector.addStatement(
                    typedSequenceElementIRI,
                    IRIs.rdfs.subClassOf,
                    IRIs.java.PrimitiveSequenceElement
                )

                // storesPrimitive Relation
                val typedStoresPrimitiveIRI =
                    IRIs.prog.genTypedStoresPrimitiveIRI(componentTypeInfo)

                tripleCollector.addStatement(
                    typedStoresPrimitiveIRI,
                    IRIs.rdf.type,
                    IRIs.owl.DatatypeProperty
                )

                tripleCollector.addStatement(
                    typedStoresPrimitiveIRI,
                    IRIs.rdf.type,
                    IRIs.owl.FunctionalProperty
                )

                tripleCollector.addStatement(
                    typedStoresPrimitiveIRI,
                    IRIs.rdfs.subPropertyOf,
                    IRIs.java.storesPrimitive
                )

                tripleCollector.addStatement(
                    typedStoresPrimitiveIRI,
                    IRIs.rdfs.domain,
                    typedSequenceElementIRI
                )

                val datatypeIRI = IRIs.java.genPrimitiveTypeIRI(componentTypeInfo)
                tripleCollector.addStatement(
                    typedStoresPrimitiveIRI,
                    IRIs.rdfs.range,
                    datatypeIRI
                )

                tripleCollector.addStatement(
                    typedSequenceElementIRI,
                    IRIs.rdfs.subClassOf,
                    tripleCollector.addConstruct(
                        TripleCollector.BlankNodeConstruct.OWLSome(
                            typedStoresPrimitiveIRI, NodeFactory.createURI(datatypeIRI)
                        )
                    )
                )
            }
            is TypeInfo.ReferenceTypeInfo -> {
                tripleCollector.addStatement(
                    typedSequenceElementIRI,
                    IRIs.rdfs.subClassOf,
                    IRIs.java.`SequenceElement%3CObject%3E`
                )

                // storesReference Relation
                val typedStoresReferenceIRI =
                    IRIs.prog.genTypedStoresReferenceIRI(componentTypeInfo)

                tripleCollector.addStatement(
                    typedStoresReferenceIRI,
                    IRIs.rdf.type,
                    IRIs.owl.ObjectProperty
                )

                tripleCollector.addStatement(
                    typedStoresReferenceIRI,
                    IRIs.rdf.type,
                    IRIs.owl.FunctionalProperty
                )

                tripleCollector.addStatement(
                    typedStoresReferenceIRI,
                    IRIs.rdfs.subPropertyOf,
                    IRIs.java.storesReference
                )

                tripleCollector.addStatement(
                    typedStoresReferenceIRI,
                    IRIs.rdfs.domain,
                    typedSequenceElementIRI
                )

                val componentTypeIRI = IRIs.prog.genReferenceTypeIRI(componentTypeInfo)
                tripleCollector.addStatement(
                    typedStoresReferenceIRI,
                    IRIs.rdfs.range,
                    withRefTypeContext(componentTypeInfo, componentTypeIRI) {
                        addReferenceOrNullClass(this)
                    }
                )

                tripleCollector.addStatement(
                    typedSequenceElementIRI,
                    IRIs.rdfs.subClassOf,
                    tripleCollector.addConstruct(
                        TripleCollector.BlankNodeConstruct.OWLSome(
                            typedStoresReferenceIRI,
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