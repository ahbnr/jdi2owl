package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers

import de.ahbnr.semanticweb.jdi2owl.Logger
import de.ahbnr.semanticweb.jdi2owl.mapping.OntIRIs
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.BuildParameters
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.pluginapi.BaseMappingListener
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TripleCollector
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface MappingContext {
    val tripleCollector: TripleCollector
    val buildParameters: BuildParameters
    val IRIs: OntIRIs
    val logger: Logger

    val pluginListeners: BaseMappingListener

    companion object {
        internal fun create(
            tripleCollector: TripleCollector,
            buildParameters: BuildParameters,
            mappingListeners: Collection<BaseMappingListener>
        ): MappingContext = MappingContextImpl(tripleCollector, buildParameters, mappingListeners)
    }
}

private class MappingContextImpl(
    override val tripleCollector: TripleCollector,
    override val buildParameters: BuildParameters,
    val mappingListeners: Collection<BaseMappingListener>
): MappingContext, KoinComponent {
    override val IRIs: OntIRIs by inject()
    override val logger: Logger by inject()

    override val pluginListeners: BaseMappingListener =
        object: BaseMappingListener {
            override fun mapInContext(context: MappingContext) =
                mappingListeners.forEach { it.mapInContext(context) }
        }
}