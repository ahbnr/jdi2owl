package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.BuildParameters
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.Mapper
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.pluginapi.BaseMappingListener
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TripleCollector
import org.apache.jena.graph.Triple
import org.apache.jena.graph.impl.GraphBase
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.util.iterator.ExtendedIterator
import org.koin.core.component.KoinComponent

internal abstract class ContextBasedMapper(
    private val mappingListeners: Collection<BaseMappingListener>
): Mapper {
    private inner class Graph(
        private val buildParameters: BuildParameters
    ) : GraphBase(), KoinComponent {
        override fun graphBaseFind(triplePattern: Triple): ExtendedIterator<Triple> {
            val tripleCollector = TripleCollector(triplePattern)

            val context = MappingContext.create(
                tripleCollector,
                buildParameters,
                mappingListeners
            )

            map(context)

            return tripleCollector.buildIterator()
        }
    }

    final override fun extendModel(buildParameters: BuildParameters, outputModel: Model) {
        val graph = Graph(buildParameters)
        val graphModel = ModelFactory.createModelForGraph(graph)

        outputModel.add(graphModel)
    }

    protected abstract fun map(context: MappingContext)
}