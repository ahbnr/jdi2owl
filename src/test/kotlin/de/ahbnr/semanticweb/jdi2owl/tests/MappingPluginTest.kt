package de.ahbnr.semanticweb.jdi2owl.tests

import de.ahbnr.semanticweb.jdi2owl.tests.utils.getTestSourceFile
import de.ahbnr.semanticweb.jdi2owl.utils.SimpleJDI2OWLApp
import org.apache.jena.rdf.model.Model
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Tests that inspect a simple HelloWorld program and confirms some properties of its structure
 */
class MappingPluginTest: TestBase() {
    companion object {
        private lateinit var rdfGraph: Model
        private lateinit var jdi2owl: SimpleJDI2OWLApp

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            jdi2owl = SimpleJDI2OWLApp(dynamicallyLoadPlugins = true)

            val result = jdi2owl.inspectClass(
                "HelloWorld",
                getTestSourceFile("de/ahbnr/semanticweb/jdi2owl/tests/HelloWorld.java"),
                4
            )

            assertThat(result.ontology).isNotNull
            this.rdfGraph = result.ontology!!.asGraphModel()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            jdi2owl.close()
        }
    }


    @Test
    fun `the DummyMapper plugin was loaded and has created the "dummy" individual in the knowledge base`() {
        val resource = rdfGraph.getResource("https://github.com/ahbnr/jdi2owl/plugins/DummyMapper#dummy")

        assertThat(rdfGraph.containsResource(resource)).isTrue()
    }
}