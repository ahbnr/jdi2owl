package de.ahbnr.semanticweb.jdi2owl.tests

import com.github.owlcs.ontapi.Ontology
import de.ahbnr.semanticweb.jdi2owl.tests.utils.getTestSourceFile
import de.ahbnr.semanticweb.jdi2owl.utils.SimpleJDI2OWLApp
import de.ahbnr.semanticweb.jdi2owl.utils.Statistics
import org.apache.jena.rdf.model.Model
import org.assertj.core.api.AbstractIntegerAssert
import org.assertj.core.api.AbstractLongAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Tests that inspect a simple HelloWorld program and confirms some properties of its structure
 */
class HelloWorldTest: TestBase() {
    companion object {
        private lateinit var rdfGraph: Model
        private lateinit var ontology: Ontology
        private lateinit var jdi2owl: SimpleJDI2OWLApp

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            jdi2owl = SimpleJDI2OWLApp()

            val result = jdi2owl.inspectClass(
                "HelloWorld",
                getTestSourceFile("de/ahbnr/semanticweb/jdi2owl/tests/HelloWorld.java"),
                4
            )

            assertThat(result.ontology).isNotNull
            this.rdfGraph = result.ontology!!.asGraphModel()
            this.ontology = result.ontology!!
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            jdi2owl.close()
        }
    }


    @Test
    fun `HelloWorld class is present`() {
        assertContainsProgResource(rdfGraph, "SysLoader-HelloWorld")
    }

    @Test
    fun `main method is present`() {
        assertContainsProgResource(rdfGraph, "SysLoader-HelloWorld.-void-main%28java.lang.String%5B%5D%29")
    }

    @Test
    fun `args parameter is present`() {
        assertContainsProgResource(rdfGraph, "SysLoader-HelloWorld.-void-main%28java.lang.String%5B%5D%29.args")
    }

    @Test
    fun `expected statistics`() {
        fun assertThat(metric: Statistics.Metric<Int>): AbstractIntegerAssert<*> = assertThat(metric.value)
        fun assertThat(metric: Statistics.Metric<Long>): AbstractLongAssert<*> = assertThat(metric.value)

        // If these numbers change a lot, either there was a big intentional change in the mapping algorithm,
        // or its a bug
        with (Statistics(ontology)) {
            assertThat(numTriples).isBetween(320_000, 350_000)
            assertThat(numAxioms).isBetween(200_000, 220_000)
            assertThat(numNamedIndividuals).isBetween(28_000, 30_000)
            assertThat(numObjectProperties).isBetween(8_000, 9_000)
            assertThat(numDataProperties).isBetween(5_500, 5_900)
            assertThat(numSubClassOfAxioms).isBetween(1_200, 1_400)
            assertThat(numEquivalentClassesAxioms).isBetween(1, 1)
            assertThat(numDisjointClassesAxioms).isBetween(1, 5)
            assertThat(numClassAssertions).isBetween(48_000, 50_000)
            assertThat(numJavaClasses).isBetween(300, 350)
            assertThat(numJavaInterfaces).isBetween(45, 55)
            assertThat(numJavaArrayTypes).isBetween(60, 70)
            assertThat(numJavaMethods).isBetween(6_400, 6_650)
            assertThat(numJavaFields).isBetween(1_450, 1_600)
            assertThat(numJavaVariableDeclarations).isBetween(12_000, 14_000)
            assertThat(numJavaObjects).isBetween(1_000, 1_200)
            assertThat(numJavaStackFrames).isEqualTo(1)
        }
    }
}