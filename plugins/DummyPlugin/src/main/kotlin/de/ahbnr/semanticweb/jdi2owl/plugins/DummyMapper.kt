package de.ahbnr.semanticweb.jdi2owl.plugins

import de.ahbnr.semanticweb.jdi2owl.mapping.OntIRIs
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.BuildParameters
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.Mapper
import org.apache.jena.rdf.model.Model
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DummyMapper: Mapper, KoinComponent {
    private val IRIs: OntIRIs by inject()

    override fun extendModel(buildParameters: BuildParameters, outputModel: Model) {
        val prefix = "https://github.com/ahbnr/jdi2owl/plugins/DummyMapper#"

        with (outputModel) {
            val subject = createResource(prefix + "dummy")
            val property = getProperty(IRIs.rdf.type)
            val `object` = getResource(IRIs.owl.NamedIndividual)

            val stmt = createStatement(subject, property, `object`)

            add(stmt)
        }
    }
}