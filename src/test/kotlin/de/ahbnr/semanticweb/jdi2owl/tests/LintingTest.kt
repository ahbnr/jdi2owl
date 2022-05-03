package de.ahbnr.semanticweb.jdi2owl.tests

import de.ahbnr.semanticweb.jdi2owl.linting.LinterMode
import de.ahbnr.semanticweb.jdi2owl.mapping.MappingLimiter
import de.ahbnr.semanticweb.jdi2owl.mapping.MappingSettings
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.GraphGenerator
import de.ahbnr.semanticweb.jdi2owl.tests.utils.getTestSourceFile
import de.ahbnr.semanticweb.jdi2owl.utils.SimpleJDI2OWLApp
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests that should confirm that there are no linting errors in a standard mapping
 */
class LintingTest: TestBase() {
    private fun genOntology(jdi2owl: SimpleJDI2OWLApp): GraphGenerator.Result {
        val result = jdi2owl.inspectClass(
            "HelloWorld",
            getTestSourceFile("de/ahbnr/semanticweb/jdi2owl/tests/HelloWorld.java"),
            4
        )
        assertNotNull(result.ontology)

        return result
    }

    @Test
    fun `No linting errors when not limiting the mapping`() {
        SimpleJDI2OWLApp().apply {
            linterMode = LinterMode.FullReport
        }.use { jdi2owl ->
            val result = genOntology(jdi2owl)

            assertTrue(result.noLints)
        }
    }

    @Test
    fun `No linting errors when limiting the mapping`() {
        SimpleJDI2OWLApp().apply {
            linterMode = LinterMode.Normal
            limiter = MappingLimiter(
                MappingSettings().apply {
                    closeReferenceTypes = false
                    makeObjectsDistinct = false

                    limitSdk = true
                }
            )
        }.use { jdi2owl ->
            val result = genOntology(jdi2owl)

            assertTrue(result.noLints)
        }
    }
}