package de.ahbnr.semanticweb.jdi2owl

import de.ahbnr.semanticweb.jdi2owl.debugging.JvmDebugger
import de.ahbnr.semanticweb.jdi2owl.linting.LinterMode
import de.ahbnr.semanticweb.jdi2owl.mapping.*
import de.ahbnr.semanticweb.jdi2owl.mapping.datatypes.JavaAccessModifierDatatype
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.*
import org.junit.jupiter.api.Test
import spoon.Launcher
import spoon.testing.utils.Check.assertNotNull
import java.nio.file.Path
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.ClassMapper
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.ObjectMapper
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.StackMapper
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFFormat
import org.apache.jena.riot.RDFWriter
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import kotlin.io.path.createDirectory

import org.koin.core.context.startKoin
import org.koin.dsl.module
import spoon.reflect.CtModel
import java.io.File

class RCNTests {
    private var debugger: JvmDebugger? = null
    private var graphGen: GraphGenerator? = null

    companion object {
        private lateinit var compilerTmpDir: Path
        private lateinit var ns: Namespaces
        private lateinit var sourceModel: CtModel
        private lateinit var limiter: MappingLimiter
        private lateinit var IRIs: OntURIs

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            ns = genDefaultNs()
            IRIs = OntURIs(ns)

            // Setup dependency injection
            @Suppress("USELESS_CAST")
            startKoin {
                modules(
                    module {
                        single { BasicLogger() as Logger }
                        single { IRIs }
                    }
                )
            }

            // Register custom datatypes with Jena
            JavaAccessModifierDatatype.register()

            val mappingSettings = MappingSettings()

            limiter = MappingLimiter(mappingSettings)

            sourceModel = Launcher().apply {
                buildModel()
            }.model

            compilerTmpDir = kotlin.io.path.createTempDirectory()
            Path.of(compilerTmpDir.toString(), "RCNTests").createDirectory()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            compilerTmpDir.toFile().deleteRecursively()
        }
    }


    @BeforeEach
    fun beforeEach() {
        debugger = JvmDebugger()

        graphGen = GraphGenerator(
            ns,
            listOf(
                ClassMapper(),
                ObjectMapper(),
                StackMapper()
            )
        )
    }

    @AfterEach
    fun afterEach() {
        debugger?.close()
    }

    private fun inspectClass(className: String, line: Int): Model {
        val filePath = Path.of(
            Thread
                .currentThread()
                .contextClassLoader
                .getResource("de/ahbnr/semanticweb/jdi2owl/RCNTests/$className.java")!!
                .toURI()
        )

        de.ahbnr.semanticweb.jdi2owl.utils.Compiler(
            listOf(filePath),
            compilerTmpDir
        ).compile()

        debugger!!.setBreakpoint("RCNTests.$className", line) {true}
        debugger!!.launchVM("RCNTests.$className", listOf(compilerTmpDir.toString()))
        debugger!!.jvm?.resume()

        val jvmState = debugger!!.jvm?.state
        assertNotNull(jvmState)

        val buildParameters = BuildParameters(
            jvmState = jvmState!!,
            sourceModel = sourceModel,
            limiter = limiter,
            typeInfoProvider = TypeInfoProvider(jvmState.pausedThread)
        )

        val ontology = graphGen!!.buildOntology(
            buildParameters,
            null,
            LinterMode.NoLinters
        )
        assertNotNull(ontology)

        return ontology!!.asGraphModel()
    }

    private fun assertContainsProgResource(rdfGraph: Model, resourceName: String) {
        assertTrue(
            rdfGraph.containsResource(
                rdfGraph.getResource(ns.prog + resourceName)
            )
        )
    }

    private fun assertContainsResource(rdfGraph: Model, rdfType: String, resourceNameRegex: Regex) {
        assertTrue(
            rdfGraph
                .listResourcesWithProperty(rdfGraph.getProperty(ns.rdf + "type"), rdfGraph.getResource(rdfType))
                .asSequence()
                .any {
                    it.isURIResource && it.uri.contains(resourceNameRegex)
                }
        )
    }

    private fun assertContainsType(rdfGraph: Model, typeNameRegex: Regex) {
        assertContainsResource(rdfGraph, ns.owl + "Class", typeNameRegex)
    }

    @Test
    fun `RCNs of top-level types are correct`() {
        val rdfGraph = inspectClass("TopLevelType", 11)

        assertContainsProgResource(rdfGraph, "SysLoader~RCNTests.TopLevelClass")
        assertContainsProgResource(rdfGraph, "SysLoader~RCNTests.TopLevelInterface")
    }

    @Test
    fun `RCNs of member types are correct`() {
        val rdfGraph = inspectClass("MemberType", 17)

        assertContainsProgResource(rdfGraph, "SysLoader~RCNTests.MyClass%24MemberClass")
        assertContainsProgResource(rdfGraph, "SysLoader~RCNTests.MyClass%24MemberInterface")
        assertContainsProgResource(rdfGraph, "SysLoader~RCNTests.MyClass%24StaticMemberClass")
    }

    @Test
    fun `RCNs of anonymous classes are correct`() {
        val rdfGraph = inspectClass("AnonymousType", 11)

        val anonClassRegex = Regex("SysLoader~RCNTests.MyClass%24\\d+$")
        assertContainsType(rdfGraph, anonClassRegex)
    }

    @Test
    fun `RCNs of local classes are correct`() {
        val rdfGraph = inspectClass("LocalType", 15)

        val localClassRegex = Regex("SysLoader~RCNTests.MyClass%24\\d+LocalClass$")
        assertContainsType(rdfGraph, localClassRegex)
    }

    @Test
    fun `RCNs of array types are correct`() {
        val rdfGraph = inspectClass("ArrayType", 11)

        assertContainsProgResource(rdfGraph, "SysLoader~RCNTests.MyClass%24StaticMemberClass%5B%5D")
    }

    @Test
    fun `RCNs of user-loaded types are correct`() {
        val rdfGraph = inspectClass("UserLoadedType", 33)

        assertContainsProgResource(rdfGraph, "SysLoader~RCNTests.UserLoadedType")
        assertContainsType(rdfGraph, Regex("Loader\\d+~RCNTests.UserLoadedType$"))
    }

    @Test
    fun `RCNs of fields are correct`() {
        val rdfGraph = inspectClass("Fields", 13)

        assertContainsProgResource(rdfGraph, "SysLoader~RCNTests.Fields.instanceField")
        assertContainsProgResource(rdfGraph, "SysLoader~RCNTests.Fields%24StaticMemberClass.staticField")
    }

    @Test
    fun `RCNs of methods are correct`() {
        val rdfGraph = inspectClass("Methods", 18)

        assertContainsProgResource(rdfGraph, "SysLoader~RCNTests.Methods.-void-someMethod%28%29")
        assertContainsProgResource(rdfGraph, "SysLoader~RCNTests.Methods.-NotYetLoaded~RCNTests.Methods%24NotLoaded-notLoadedTypesMethod%28NotYetLoaded~RCNTests.Methods%24NotLoaded%29")
        assertContainsProgResource(rdfGraph, "SysLoader~RCNTests.Methods%24StaticMemberClass.-void-someMethod%28%29")
        assertContainsProgResource(rdfGraph, "SysLoader~RCNTests.Methods%24StaticMemberClass.-SysLoader~RCNTests.Methods%24StaticMemberClass-complexMethod%28SysLoader~RCNTests.Methods%24StaticMemberClass%2CSysLoader~RCNTests.Methods%24StaticMemberClass%29")
    }

    @Test
    fun `RCNs of variables are correct`() {
        val rdfGraph = inspectClass("Variables", 21)
        RDFWriter
            .create(rdfGraph)
            .lang(Lang.TURTLE)
            .format(RDFFormat.TURTLE_PRETTY)
            .output(File("datatest.ttl").outputStream())

        assertContainsProgResource(rdfGraph, "SysLoader~RCNTests.Variables.-void-someMethod%28%29.myVar")
        assertContainsResource(
            rdfGraph,
            IRIs.java.VariableDeclaration,
            Regex("SysLoader~RCNTests.Variables.-void-sameVarTwice%28%29.myVar~\\d+$")
        )
    }
}