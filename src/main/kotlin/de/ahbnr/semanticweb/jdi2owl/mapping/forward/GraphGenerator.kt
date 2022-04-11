@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE", "JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE")

package de.ahbnr.semanticweb.jdi2owl.mapping.forward

import com.github.owlcs.ontapi.OntManagers
import com.github.owlcs.ontapi.Ontology
import de.ahbnr.semanticweb.jdi2owl.linting.LinterMode
import de.ahbnr.semanticweb.jdi2owl.linting.ModelSanityChecker
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.macros.Chain
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.UniversalKnowledgeBaseParser
import de.ahbnr.semanticweb.jdi2owl.Logger
import de.ahbnr.semanticweb.jdi2owl.mapping.OntURIs
import org.apache.jena.rdf.model.Model
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.semanticweb.owlapi.model.UnloadableImportException
import org.semanticweb.owlapi.util.AutoIRIMapper
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Path

class ParserException: Exception()

class GraphGenerator(
    private val mappers: List<IMapper>
) : KoinComponent {
    private val logger: Logger by inject()
    private val IRIs: OntURIs by inject()

    private val macros = arrayOf(Chain())

    data class Result(
        val ontology: Ontology?,
        val noLints: Boolean
    )

    private fun readIntoModel(fileName: String?, model: Model, inputStreamProducer: () -> InputStream) {
        val reader = UniversalKnowledgeBaseParser(model, fileName, inputStreamProducer)
        reader.readIntoModel()
    }

    private fun loadJavaOntology(model: Model) {
        val resourcePath = "/de/ahbnr/semanticweb/jdi2owl/ontologies/java.ttl"
        val inputStreamProducer = { javaClass.getResourceAsStream(resourcePath)!! }

        readIntoModel(resourcePath, model, inputStreamProducer)
    }

    private fun loadMacrosOntology(model: Model) {
        val resourcePath = "/de/ahbnr/semanticweb/jdi2owl/ontologies/macros.ttl"
        val inputStreamProducer = { javaClass.getResourceAsStream(resourcePath)!! }

        readIntoModel(resourcePath, model, inputStreamProducer)
    }

    private fun mapProgramState(buildParameters: BuildParameters, model: Model) {
        for (mapper in mappers) {
            mapper.extendModel(buildParameters, model)
        }
    }

    private fun loadApplicationDomain(
        applicationDomainRulesPath: String?, /* turtle format file */
        model: Model
    ) {
        if (applicationDomainRulesPath != null) {
            val fileStreamProducer = { FileInputStream(File(applicationDomainRulesPath)) }
            readIntoModel(applicationDomainRulesPath, model, fileStreamProducer)
        }
    }

    fun buildOntology(
        buildParameters: BuildParameters,
        applicationDomainRulesPath: String?, /* turtle format file */
        linterMode: LinterMode
    ): Result {
        val ontManager = OntManagers.createManager()
        // Also search imports in current working directory
        ontManager.iriMappers.add(AutoIRIMapper(Path.of("").toFile(), false))

        val ontology = ontManager.createOntology()

        val model: Model = ontology.asGraphModel()

        try {
            // Declare dynamically generated prefixes
            model.setNsPrefix("run", IRIs.ns.run)
            model.setNsPrefix("local", IRIs.ns.local)

            // Load Java ontology
            loadJavaOntology(model)

            // Load Macros ontology
            loadMacrosOntology(model)

            // Map program state
            mapProgramState(buildParameters, model)

            // Load application domain knowledge
            loadApplicationDomain(applicationDomainRulesPath, model)

            // Load imports
            ontology
                .importsDeclarations()
                .forEach {
                    try {
                        ontology.owlOntologyManager.loadOntology(it.iri)
                    } catch (e: UnloadableImportException) {
                        logger.error(e.message ?: "Could not load one of the imported ontologies.")
                    }
                }

            // Execute Macros
            for (macro in macros) {
                macro.executeAll(ontology.asGraphModel())
            }
        } catch (e: ParserException) {
            logger.error("Aborted model building due to fatal parser error.")
            return Result(null, false)
        }

        // Perform sanity checks and linting
        val checker = ModelSanityChecker()
        val noLints = checker.fullCheck(ontology, buildParameters.limiter, linterMode)

        return Result(ontology, noLints)
    }
}