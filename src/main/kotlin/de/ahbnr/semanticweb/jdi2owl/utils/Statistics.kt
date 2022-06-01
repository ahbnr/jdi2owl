package de.ahbnr.semanticweb.jdi2owl.utils

import com.github.owlcs.ontapi.Ontology
import de.ahbnr.semanticweb.jdi2owl.mapping.OntIRIs
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.semanticweb.owlapi.metrics.*
import org.semanticweb.owlapi.model.AxiomType


class Statistics(ontology: Ontology): KoinComponent {
    private val longMetrics: Map<String, Metric<Long>>
    private val intMetrics: Map<String, Metric<Int>>
    val allMetrics: Map<String, Metric<*>>
    init {
        with(get<OntIRIs>()) {
            val rdfGraph = ontology.asGraphModel()
            val typeProperty = rdfGraph.getProperty(rdf.type)

            fun countSubjects(iri: String): Int {
                val objectWithIRI = rdfGraph.getResource(iri)
                val subjects = rdfGraph.listSubjectsWithProperty(typeProperty, objectWithIRI)

                return subjects.asSequence().count()
            }

            this@Statistics.longMetrics = mapOf(
                "numTriples" to Metric("Triples", ontology.asGraphModel().statements().count()),
            )

            this@Statistics.intMetrics = mapOf(
                "numAxioms" to Metric("Axioms", AxiomCount(ontology).value),
                "numNamedIndividuals" to Metric("Named Individuals", ReferencedIndividualCount(ontology).value),
                "numObjectProperties" to Metric("Object Properties", ReferencedObjectPropertyCount(ontology).value),
                "numDataProperties" to Metric("Data Properties", ReferencedDataPropertyCount(ontology).value),
                "numSubClassOfAxioms" to Metric("SubClassOf Axioms", AxiomTypeMetric(ontology, AxiomType.SUBCLASS_OF).value),
                "numEquivalentClassesAxioms" to Metric("Equivalent Classes Axioms", AxiomTypeMetric(ontology, AxiomType.EQUIVALENT_CLASSES).value),
                "numDisjointClassesAxioms" to Metric("Disjoint Classes Axioms", AxiomTypeMetric(ontology, AxiomType.DISJOINT_CLASSES).value),
                "numClassAssertions" to Metric("Class Assertion Axioms", AxiomTypeMetric(ontology, AxiomType.CLASS_ASSERTION).value),

                "numJavaClasses" to Metric("Java Classes", countSubjects(java.Class)),
                "numJavaInterfaces" to Metric("Java Interfaces", countSubjects(java.Interface)),
                "numJavaArrayTypes" to Metric("Java Array Types", countSubjects(java.ArrayType)),
                "numJavaMethods" to Metric("Java Methods", countSubjects(java.Method)),
                "numJavaFields" to Metric("Java Fields", countSubjects(java.Field)),
                "numJavaVariableDeclarations" to Metric("Java Varianbles", countSubjects(java.VariableDeclaration)),
                "numJavaObjects" to Metric("Java Objects", countSubjects(java.Object)),
                "numJavaStackFrames" to Metric("Java Stack Frames", countSubjects(java.StackFrame)),
            )

            allMetrics = longMetrics + intMetrics
        }
    }

    // generic RDF statistics
    // (lower
    val numTriples by longMetrics

    // generic OWL statistics
    val numAxioms by intMetrics
    val numNamedIndividuals by intMetrics
    val numObjectProperties by intMetrics
    val numDataProperties by intMetrics
    val numSubClassOfAxioms by intMetrics
    val numEquivalentClassesAxioms by intMetrics
    val numDisjointClassesAxioms by intMetrics
    val numClassAssertions by intMetrics

    // Java specific properties
    val numJavaClasses by intMetrics
    val numJavaInterfaces by intMetrics
    val numJavaArrayTypes by intMetrics
    val numJavaMethods by intMetrics
    val numJavaFields by intMetrics
    val numJavaVariableDeclarations by intMetrics
    val numJavaObjects by intMetrics
    val numJavaStackFrames by intMetrics

    class Metric<T>(val name: String, val value: T)
}