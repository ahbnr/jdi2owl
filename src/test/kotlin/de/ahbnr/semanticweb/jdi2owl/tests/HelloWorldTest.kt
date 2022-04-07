package de.ahbnr.semanticweb.jdi2owl.tests

import de.ahbnr.semanticweb.jdi2owl.tests.utils.getTestSourceFile
import de.ahbnr.semanticweb.jdi2owl.utils.SimpleJDI2OWLApp
import org.apache.jena.rdf.model.Model
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import spoon.testing.utils.Check

/**
 * Tests that inspect a simple HelloWorld program and confirms some properties of its structure
 */
class HelloWorldTest: TestBase() {
    companion object {
        private lateinit var rdfGraph: Model
        private lateinit var jdi2owl: SimpleJDI2OWLApp

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            jdi2owl = SimpleJDI2OWLApp()

            val ontology = jdi2owl.inspectClass(
                "HelloWorld",
                getTestSourceFile("de/ahbnr/semanticweb/jdi2owl/tests/HelloWorld.java"),
                4
            )

            Check.assertNotNull(ontology)
            this.rdfGraph = ontology!!.asGraphModel()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            jdi2owl.close()
        }
    }


    @Test
    fun `HelloWorld class is present`() {
        assertContainsProgResource(rdfGraph, "SysLoader~HelloWorld")
    }

    @Test
    fun `main method is present`() {
        assertContainsProgResource(rdfGraph, "SysLoader~HelloWorld.-void-main%28java.lang.String%5B%5D%29")
    }

    @Test
    fun `args parameter is present`() {
        assertContainsProgResource(rdfGraph, "SysLoader~HelloWorld.-void-main%28java.lang.String%5B%5D%29.args")
    }
}