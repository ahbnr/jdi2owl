package de.ahbnr.semanticweb.jdi2owl.tests

import de.ahbnr.semanticweb.jdi2owl.tests.utils.getTestSourceFile
import de.ahbnr.semanticweb.jdi2owl.utils.SimpleJDI2OWLApp
import org.apache.jena.rdf.model.Model
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

class RCNTests: TestBase() {
    companion object {
        private lateinit var jdi2owl: SimpleJDI2OWLApp

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            jdi2owl = SimpleJDI2OWLApp()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            jdi2owl.close()
        }
    }

    private fun inspectClass(className: String, line: Int): Model {
        val result = jdi2owl.inspectClass(
            "RCNTests.$className",
            getTestSourceFile("de/ahbnr/semanticweb/jdi2owl/tests/RCNTests/$className.java"),
            line
        )

        assertNotNull(result.ontology)

        return result.ontology!!.asGraphModel()
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    fun `RCNs of top-level types are correct`() {
        val rdfGraph = inspectClass("TopLevelType", 11)

        assertContainsProgResource(rdfGraph, "SysLoader-RCNTests.TopLevelClass")
        assertContainsProgResource(rdfGraph, "SysLoader-RCNTests.TopLevelInterface")
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    fun `RCNs of member types are correct`() {
        val rdfGraph = inspectClass("MemberType", 17)

        assertContainsProgResource(rdfGraph, "SysLoader-RCNTests.MyClass%24MemberClass")
        assertContainsProgResource(rdfGraph, "SysLoader-RCNTests.MyClass%24MemberInterface")
        assertContainsProgResource(rdfGraph, "SysLoader-RCNTests.MyClass%24StaticMemberClass")
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    fun `RCNs of anonymous classes are correct`() {
        val rdfGraph = inspectClass("AnonymousType", 11)

        val anonClassRegex = Regex("SysLoader-RCNTests.MyClass%24\\d+$")
        assertContainsType(rdfGraph, anonClassRegex)
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    fun `RCNs of local classes are correct`() {
        val rdfGraph = inspectClass("LocalType", 15)

        val localClassRegex = Regex("SysLoader-RCNTests.MyClass%24\\d+LocalClass$")
        assertContainsType(rdfGraph, localClassRegex)
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    fun `RCNs of array types are correct`() {
        val rdfGraph = inspectClass("ArrayType", 11)

        assertContainsProgResource(rdfGraph, "SysLoader-RCNTests.MyClass%24StaticMemberClass%5B%5D")
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    fun `RCNs of user-loaded types are correct`() {
        val rdfGraph = inspectClass("UserLoadedType", 33)

        assertContainsProgResource(rdfGraph, "SysLoader-RCNTests.UserLoadedType")
        assertContainsType(rdfGraph, Regex("Loader\\d+-RCNTests.UserLoadedType$"))
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    fun `RCNs of fields are correct`() {
        val rdfGraph = inspectClass("Fields", 13)

        assertContainsProgResource(rdfGraph, "SysLoader-RCNTests.Fields.instanceField")
        assertContainsProgResource(rdfGraph, "SysLoader-RCNTests.Fields%24StaticMemberClass.staticField")
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    fun `RCNs of methods are correct`() {
        val rdfGraph = inspectClass("Methods", 18)

        assertContainsProgResource(rdfGraph, "SysLoader-RCNTests.Methods.-void-someMethod%28%29")
        assertContainsProgResource(rdfGraph, "SysLoader-RCNTests.Methods.-Unprepared-RCNTests.Methods%24NotLoaded-notLoadedTypesMethod%28Unprepared-RCNTests.Methods%24NotLoaded%29")
        assertContainsProgResource(rdfGraph, "SysLoader-RCNTests.Methods%24StaticMemberClass.-void-someMethod%28%29")
        assertContainsProgResource(rdfGraph, "SysLoader-RCNTests.Methods%24StaticMemberClass.-SysLoader-RCNTests.Methods%24StaticMemberClass-complexMethod%28SysLoader-RCNTests.Methods%24StaticMemberClass%2CSysLoader-RCNTests.Methods%24StaticMemberClass%29")
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    fun `RCNs of constructors are correct`() {
        val rdfGraph = inspectClass("Constructors", 13)

        assertContainsProgResource(rdfGraph, "SysLoader-RCNTests.Constructors%24StaticMemberClass.-void-%3Cinit%3E%28%29")
        assertContainsProgResource(rdfGraph, "SysLoader-RCNTests.Constructors%24StaticMemberClass.-void-%3Cinit%3E%28SysLoader-RCNTests.Constructors%24StaticMemberClass%2CSysLoader-RCNTests.Constructors%24StaticMemberClass%29")
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    fun `RCNs of variables are correct`() {
        val rdfGraph = inspectClass("Variables", 21)

        assertContainsProgResource(rdfGraph, "SysLoader-RCNTests.Variables.-void-someMethod%28%29.myVar")
        assertContainsResource(
            rdfGraph,
            IRIs.java.VariableDeclaration,
            Regex("SysLoader-RCNTests.Variables.-void-sameVarTwice%28%29.myVar-\\d+$")
        )
    }
}