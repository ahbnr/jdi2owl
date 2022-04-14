package de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils

import org.apache.jena.graph.Node

/**
 * Provides a nice DSL syntax for adding triples to a TripleCollector
 */
class TripleCollectorDSL(private val tripleCollector: TripleCollector) {
    class SubjectContext(private val tripleCollector: TripleCollector, private val subjectIRI: String) {
        infix fun String.of(objectIRI: String) {
            tripleCollector.addStatement(subjectIRI, this, objectIRI)
        }

        infix fun String.of(objectNode: Node) {
            tripleCollector.addStatement(subjectIRI, this, objectNode)
        }
    }

    operator fun String.invoke(block: SubjectContext.() -> Unit) {
        SubjectContext(tripleCollector, this).block()
    }
}