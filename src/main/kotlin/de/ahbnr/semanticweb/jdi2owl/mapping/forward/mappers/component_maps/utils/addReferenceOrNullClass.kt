package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.utils

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure.RefTypeContext
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TripleCollector
import org.apache.jena.graph.NodeFactory

fun addReferenceOrNullClass(context: RefTypeContext) =
    with(context) {
        // every reference can either be an instance or null:
        // fieldTypeSubject ∪ { java:null }
        // [ owl:unionOf ( fieldTypeSubject [ owl:oneOf ( java:null ) ] ) ] .
        tripleCollector.addConstruct(
            TripleCollector.BlankNodeConstruct.OWLUnion(
                listOf(
                    NodeFactory.createURI(typeIRI),

                    tripleCollector.addConstruct(
                        TripleCollector.BlankNodeConstruct.OWLOneOf.fromIRIs(listOf(IRIs.java.`null`))
                    )
                )
            )
        )
    }
