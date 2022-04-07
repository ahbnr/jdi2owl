package de.ahbnr.semanticweb.jdi2owl.tests

import de.ahbnr.semanticweb.jdi2owl.mapping.OntURIs
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFFormat
import org.apache.jena.riot.RDFWriter
import org.junit.jupiter.api.Assertions
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

open class TestBase: KoinComponent {
    protected val IRIs: OntURIs by inject()

    protected fun dumpRDFGraph(rdfGraph: Model, filePath: String) {
        RDFWriter.create(rdfGraph)
            .lang(Lang.TURTLE)
            .format(RDFFormat.TURTLE_PRETTY)
            .output(File(filePath).outputStream())
    }

    protected fun assertContainsProgResource(rdfGraph: Model, resourceName: String) {
        val iri = IRIs.ns.prog + resourceName

        Assertions.assertTrue(
            rdfGraph.containsResource(
                rdfGraph.getResource(iri)
            ),
            "Expected resource $iri in RDF graph but it could not be found."
        )
    }

    protected fun assertContainsResource(rdfGraph: Model, rdfType: String, resourceNameRegex: Regex) {
        Assertions.assertTrue(
            rdfGraph
                .listResourcesWithProperty(rdfGraph.getProperty(IRIs.rdf.type), rdfGraph.getResource(rdfType))
                .asSequence()
                .any {
                    it.isURIResource && it.uri.contains(resourceNameRegex)
                }
        )
    }

    protected fun assertContainsType(rdfGraph: Model, typeNameRegex: Regex) {
        assertContainsResource(rdfGraph, IRIs.owl.Class, typeNameRegex)
    }
}