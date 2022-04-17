package de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils

import de.ahbnr.semanticweb.jdi2owl.mapping.OntIRIs
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.Node
import org.apache.jena.graph.NodeFactory
import org.apache.jena.graph.Triple
import org.apache.jena.util.iterator.ExtendedIterator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TripleCollector(private val triplePattern: Triple) : KoinComponent {
    private val IRIs: OntIRIs by inject()

    private val collectedTriples = mutableSetOf<Triple>()

    sealed class BlankNodeConstruct {
        data class RDFList(val objects: List<Node>) : BlankNodeConstruct() {
            companion object {
                fun fromIRIs(objects: List<String>) =
                    RDFList(objects.map { NodeFactory.createURI(it) })
            }
        }

        data class OWLUnion(val objects: List<Node>) : BlankNodeConstruct() {
            companion object {
                fun fromIRIs(objects: List<String>) =
                    OWLUnion(objects.map { NodeFactory.createURI(it) })
            }
        }

        data class OWLOneOf(val objects: List<Node>) : BlankNodeConstruct() {
            companion object {
                fun fromIRIs(objects: List<String>) =
                    OWLOneOf(objects.map { NodeFactory.createURI(it) })
            }
        }

        class OWLSome : BlankNodeConstruct {
            val property: Node
            val some: Node

            constructor(property: Node, some: Node) {
                this.property = property
                this.some = some
            }

            constructor(propertyUri: String, some: Node) {
                this.property = NodeFactory.createURI(propertyUri)
                this.some = some
            }
        }

        sealed class CardinalityType {
            data class Exactly(val value: Int) : CardinalityType()
        }

        class OWLObjectCardinalityRestriction: BlankNodeConstruct {
            val onProperty: Node
            val onClassUri: String
            val cardinality: CardinalityType

            constructor(
                onPropertyUri: String,
                onClassUri: String,
                cardinality: CardinalityType
            ) {
                this.onProperty = NodeFactory.createURI(onPropertyUri)
                this.onClassUri = onClassUri
                this.cardinality = cardinality
            }

            constructor(
                onProperty: Node,
                onClassUri: String,
                cardinality: CardinalityType
            ) {
                this.onProperty = onProperty
                this.onClassUri = onClassUri
                this.cardinality = cardinality
            }
        }

        data class OWLDataCardinalityRestriction(
            val onPropertyUri: String,
            val cardinality: CardinalityType
        ) : BlankNodeConstruct()

        class OWLInverseProperty(
            val basePropertyUri: String
        ) : BlankNodeConstruct()
    }

    fun addStatement(subject: Node, predicate: Node, `object`: Node) {
        val candidateTriple = Triple(subject, predicate, `object`)

        if (triplePattern.matches(candidateTriple)) {
            collectedTriples.add(candidateTriple)
        }
    }

    fun addStatement(subject: Node, predicate: String, `object`: Node) {
        addStatement(
            subject,
            NodeFactory.createURI(predicate),
            `object`
        )
    }

    fun addStatement(subject: String, predicate: String, `object`: Node) {
        addStatement(
            NodeFactory.createURI(subject),
            NodeFactory.createURI(predicate),
            `object`
        )
    }

    fun addStatement(subject: Node, predicate: String, obj: String) {
        addStatement(
            subject,
            NodeFactory.createURI(predicate),
            NodeFactory.createURI(obj)
        )
    }

    fun addStatement(subject: String, predicate: String, obj: String) {
        addStatement(
            subject,
            predicate,
            NodeFactory.createURI(obj)
        )
    }

    fun addConstruct(blankNodeConstruct: BlankNodeConstruct): Node =
        when (blankNodeConstruct) {
            is BlankNodeConstruct.RDFList -> addListStatements(
                blankNodeConstruct.objects
            )
            is BlankNodeConstruct.OWLUnion -> addUnionStatements(
                blankNodeConstruct.objects
            )
            is BlankNodeConstruct.OWLOneOf -> addOneOfStatements(
                blankNodeConstruct.objects
            )
            is BlankNodeConstruct.OWLSome -> addSomeRestriction(blankNodeConstruct)
            is BlankNodeConstruct.OWLObjectCardinalityRestriction -> addObjectCardinalityRestriction(blankNodeConstruct)
            is BlankNodeConstruct.OWLDataCardinalityRestriction -> addDataCardinalityRestriction(blankNodeConstruct)
            is BlankNodeConstruct.OWLInverseProperty -> addInverseProperty(blankNodeConstruct)
        }

    private fun addObjectCardinalityRestriction(cardinalityRestriction: BlankNodeConstruct.OWLObjectCardinalityRestriction): Node {
        val restrictionNode = NodeFactory.createBlankNode()

        addStatement(
            restrictionNode,
            IRIs.rdf.type,
            IRIs.owl.Restriction
        )

        addStatement(
            restrictionNode,
            IRIs.owl.onProperty,
            cardinalityRestriction.onProperty
        )

        addStatement(
            restrictionNode,
            IRIs.owl.onClass,
            cardinalityRestriction.onClassUri
        )

        when (cardinalityRestriction.cardinality) {
            is BlankNodeConstruct.CardinalityType.Exactly ->
                addStatement(
                    restrictionNode,
                    IRIs.owl.cardinality, // FIXME: This should be owl:qualifiedCardinality shouldnt it?
                    NodeFactory.createLiteralByValue(
                        cardinalityRestriction.cardinality.value,
                        XSDDatatype.XSDnonNegativeInteger
                    )
                )
        }

        return restrictionNode
    }

    private fun addDataCardinalityRestriction(cardinalityRestriction: BlankNodeConstruct.OWLDataCardinalityRestriction): Node {
        val restrictionNode = NodeFactory.createBlankNode()

        addStatement(
            restrictionNode,
            IRIs.rdf.type,
            IRIs.owl.Restriction
        )

        addStatement(
            restrictionNode,
            IRIs.owl.onProperty,
            cardinalityRestriction.onPropertyUri
        )

        when (cardinalityRestriction.cardinality) {
            is BlankNodeConstruct.CardinalityType.Exactly ->
                addStatement(
                    restrictionNode,
                    IRIs.owl.cardinality, // FIXME: This should be owl:qualifiedCardinality shouldnt it?
                    NodeFactory.createLiteralByValue(
                        cardinalityRestriction.cardinality.value,
                        XSDDatatype.XSDnonNegativeInteger
                    )
                )
        }

        return restrictionNode
    }

    private fun addInverseProperty(inversePropertyConstruct: BlankNodeConstruct.OWLInverseProperty): Node {
        val blankNode = NodeFactory.createBlankNode()

        addStatement(
            blankNode,
            IRIs.rdf.type,
            IRIs.owl.ObjectProperty
        )

        addStatement(
            blankNode,
            IRIs.owl.inverseOf,
            inversePropertyConstruct.basePropertyUri
        )

        return blankNode
    }

    private fun addSomeRestriction(owlSome: BlankNodeConstruct.OWLSome): Node {
        val restrictionNode = NodeFactory.createBlankNode()

        addStatement(
            restrictionNode,
            IRIs.rdf.type,
            IRIs.owl.Restriction
        )

        addStatement(
            restrictionNode,
            IRIs.owl.onProperty,
            owlSome.property
        )

        addStatement(
            restrictionNode,
            IRIs.owl.someValuesFrom,
            owlSome.some
        )

        return restrictionNode
    }

    private fun addOneOfStatements(objectList: List<Node>): Node {
        val oneOfNode = NodeFactory.createBlankNode()

        addStatement(
            oneOfNode,
            NodeFactory.createURI(IRIs.owl.oneOf),
            addListStatements(objectList)
        )

        // Protege will not recognize the oneOf relation if we dont add this "is a class" declaration
        addStatement(
            oneOfNode,
            IRIs.rdf.type,
            IRIs.owl.Class
        )

        return oneOfNode
    }

    private fun addUnionStatements(objectList: List<Node>): Node {
        val unionNode = NodeFactory.createBlankNode()

        addStatement(
            unionNode,
            IRIs.rdf.type,
            IRIs.owl.Class
        )

        addStatement(
            unionNode,
            NodeFactory.createURI(IRIs.owl.unionOf),
            addListStatements(objectList)
        )

        return unionNode
    }

    private fun addListStatements(objectList: List<Node>): Node {
        val firstObject = objectList.firstOrNull()

        return if (firstObject == null) {
            val root = NodeFactory.createURI(IRIs.rdf.nil)

            root
        } else {
            val root = NodeFactory.createBlankNode()
            for (candidateTriple in genList(root, firstObject, objectList.drop(1))) {
                if (triplePattern.matches(candidateTriple)) {
                    collectedTriples.add(candidateTriple)
                }
            }

            root
        }
    }

    fun dsl(block: TripleCollectorDSL.() -> Unit) {
        TripleCollectorDSL(this).block()
    }

    private fun genList(root: Node, first: Node, rest: List<Node>): Sequence<Triple> = sequence {
        yield(Triple(root, NodeFactory.createURI(IRIs.rdf.type), NodeFactory.createURI(IRIs.rdf.List)))
        yield(Triple(root, NodeFactory.createURI(IRIs.rdf.first), first))

        val firstOfRest = rest.firstOrNull()
        if (firstOfRest == null) {
            yield(Triple(root, NodeFactory.createURI(IRIs.rdf.rest), NodeFactory.createURI(IRIs.rdf.nil)))
        } else {
            val restRoot = NodeFactory.createBlankNode()
            yield(Triple(root, NodeFactory.createURI(IRIs.rdf.rest), restRoot))
            yieldAll(genList(restRoot, firstOfRest, rest.drop(1)))
        }
    }

    fun buildIterator(): ExtendedIterator<Triple> = TripleIterableIterator(collectedTriples)
}