package de.ahbnr.semanticweb.jdi2owl.tests

import de.ahbnr.semanticweb.jdi2owl.linting.LinterMode
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.GraphGenerator
import de.ahbnr.semanticweb.jdi2owl.tests.utils.getTestSourceFile
import de.ahbnr.semanticweb.jdi2owl.utils.SimpleJDI2OWLApp
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Tests that should confirm that there are no linting errors in a standard mapping
 */
class LintingTest: TestBase() {
    companion object {
        private lateinit var result: GraphGenerator.Result
        private lateinit var jdi2owl: SimpleJDI2OWLApp

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            jdi2owl = SimpleJDI2OWLApp().apply {
                linterMode = LinterMode.FullReport
            }

            result = jdi2owl.inspectClass(
                "HelloWorld",
                getTestSourceFile("de/ahbnr/semanticweb/jdi2owl/tests/HelloWorld.java"),
                4
            )

            assertNotNull(result.ontology)
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            jdi2owl.close()
        }
    }


    @Test
    fun `No linting errors`() {
        dumpRDFGraph(result.ontology!!.asGraphModel(), "datatest.ttl")
        assertTrue(result.noLints)
    }
}