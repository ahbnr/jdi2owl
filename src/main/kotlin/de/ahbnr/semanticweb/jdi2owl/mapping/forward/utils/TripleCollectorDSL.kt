package de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils

import org.apache.jena.graph.Node
import org.apache.jena.graph.NodeFactory

/**
 * Provides a nice DSL syntax for adding triples to a TripleCollector
 */
class TripleCollectorDSL(private val tripleCollector: TripleCollector) {
    inner class SubjectContext(private val tripleCollector: TripleCollector, private val subjectIRI: String) {
        infix fun String.of(objectIRI: String) {
            tripleCollector.addStatement(subjectIRI, this, objectIRI)
        }

        infix fun String.of(objectNode: Node) {
            tripleCollector.addStatement(subjectIRI, this, objectNode)
        }
    }

    infix fun String.`⊔`(rhs: Node) =
        NodeFactory.createURI(this) `⊔` rhs

    infix fun Node.`⊔`(rhs: Node) =
        tripleCollector.addConstruct(
            TripleCollector.BlankNodeConstruct.OWLUnion(
                listOf(
                    this, rhs
                )
            )
        )

    fun oneOf(vararg iris: String) =
        tripleCollector.addConstruct(
            TripleCollector.BlankNodeConstruct.OWLOneOf.fromIRIs(
                listOf( *iris )
            )
        )

    operator fun String.invoke(block: SubjectContext.() -> Unit) {
        SubjectContext(tripleCollector, this).block()
    }
}