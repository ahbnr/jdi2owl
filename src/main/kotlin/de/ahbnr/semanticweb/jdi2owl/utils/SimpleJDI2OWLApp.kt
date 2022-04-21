package de.ahbnr.semanticweb.jdi2owl.utils

import de.ahbnr.semanticweb.jdi2owl.BasicLogger
import de.ahbnr.semanticweb.jdi2owl.Logger
import de.ahbnr.semanticweb.jdi2owl.debugging.JvmDebugger
import de.ahbnr.semanticweb.jdi2owl.linting.LinterMode
import de.ahbnr.semanticweb.jdi2owl.mapping.*
import de.ahbnr.semanticweb.jdi2owl.mapping.datatypes.JavaAccessModifierDatatype
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.*
import spoon.Launcher
import java.nio.file.Path
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TypeInfoProvider

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import spoon.reflect.CtModel

class SimpleJDI2OWLApp: AutoCloseable {
    private val logger = BasicLogger()
    private val sourceModel: CtModel
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

        sourceModel = Launcher().apply {
            buildModel()
        }.model
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
            val mappers = IMapper.getAllMappers()
            val graphGen = GraphGenerator( mappers )

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
                sourceModel = sourceModel,
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