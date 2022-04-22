package de.ahbnr.semanticweb.jdi2owl.utils

import de.ahbnr.semanticweb.jdi2owl.BasicLogger
import de.ahbnr.semanticweb.jdi2owl.Logger
import de.ahbnr.semanticweb.jdi2owl.debugging.JvmDebugger
import de.ahbnr.semanticweb.jdi2owl.linting.LinterMode
import de.ahbnr.semanticweb.jdi2owl.mapping.MappingLimiter
import de.ahbnr.semanticweb.jdi2owl.mapping.MappingSettings
import de.ahbnr.semanticweb.jdi2owl.mapping.OntIRIs
import de.ahbnr.semanticweb.jdi2owl.mapping.datatypes.JavaAccessModifierDatatype
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.BaseMapping
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.BuildParameters
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.GraphGenerator
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.MappingWithPlugins
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TypeInfoProvider
import de.ahbnr.semanticweb.jdi2owl.mapping.genDefaultNs
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.nio.file.Path

class SimpleJDI2OWLApp(
    private val dynamicallyLoadPlugins: Boolean = false
): AutoCloseable {
    private val logger = BasicLogger()
    private val limiter: MappingLimiter

    var linterMode: LinterMode = LinterMode.NoLinters

    init {
        // Setup dependency injection
        @Suppress("USELESS_CAST")
        startKoin {
            modules(
                module {
                    single { this@SimpleJDI2OWLApp.logger as Logger }
                    single { OntIRIs(genDefaultNs()) }
                }
            )
        }

        // Register custom datatypes with Jena
        JavaAccessModifierDatatype.register()

        // Prepare settings for mapping
        val mappingSettings = MappingSettings()
        limiter = MappingLimiter(mappingSettings)
    }

    override fun close() {
        stopKoin()
    }

    fun inspectClass(className: String, filePath: Path, line: Int): GraphGenerator.Result {
        // Prepare temporary directory for storing compiled classes
        val compilerTmpDir = kotlin.io.path.createTempDirectory()

        try {
            Compiler(
                listOf(filePath),
                compilerTmpDir
            ).compile()

            return inspectClass(className, listOf(compilerTmpDir.toString()), line)
        }

        finally {
            compilerTmpDir.toFile().deleteRecursively()
        }
    }

    fun inspectClass(className: String, classpaths: List<String>, line: Int): GraphGenerator.Result {
        return JvmDebugger().use { debugger ->
            val mappingMode =
                if (dynamicallyLoadPlugins)
                    MappingWithPlugins(
                        emptyList(),
                        true
                    )
                else
                    BaseMapping
            val graphGen = GraphGenerator( mappingMode )

            debugger.setBreakpoint(className, line) {true}
            debugger.launchVM(className, classpaths)
            debugger.jvm?.resume()

            val jvmState = debugger.jvm?.state
                ?: run {
                    logger.error("Program was not suspended. Can not obtain JVM state.")
                    return GraphGenerator.Result(null, true)
                }

            val buildParameters = BuildParameters(
                jvmState = jvmState,
                limiter = limiter,
                typeInfoProvider = TypeInfoProvider(jvmState.pausedThread)
            )

            val result = graphGen.buildOntology(
                buildParameters,
                null,
                linterMode
            )

            result
        }
    }
}