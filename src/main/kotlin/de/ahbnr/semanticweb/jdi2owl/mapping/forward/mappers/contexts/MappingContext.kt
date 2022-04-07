package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.contexts

import de.ahbnr.semanticweb.jdi2owl.Logger
import de.ahbnr.semanticweb.jdi2owl.mapping.OntURIs
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.BuildParameters
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TripleCollector
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface MappingContext {
    val tripleCollector: TripleCollector
    val buildParameters: BuildParameters
    val IRIs: OntURIs
    val logger: Logger

    companion object {
        fun create(
            tripleCollector: TripleCollector,
            buildParameters: BuildParameters
        ) = MappingContextImpl(tripleCollector, buildParameters)
    }
}

class MappingContextImpl(
    override val tripleCollector: TripleCollector,
    override val buildParameters: BuildParameters
): MappingContext, KoinComponent {
    override val IRIs: OntURIs by inject()
    override val logger: Logger by inject()
}