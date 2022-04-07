package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.BuildParameters
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.IMapper
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure.mapReferenceTypes
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.contexts.MappingContext
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TripleCollector
import org.apache.jena.graph.Triple
import org.apache.jena.graph.impl.GraphBase
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.util.iterator.ExtendedIterator
import org.koin.core.component.KoinComponent

class ClassMapper : IMapper {
    private class Graph(
        private val buildParameters: BuildParameters
    ) : GraphBase(), KoinComponent {
        override fun graphBaseFind(triplePattern: Triple): ExtendedIterator<Triple> {
            val tripleCollector = TripleCollector(triplePattern)

            val context = MappingContext.create(
                tripleCollector,
                buildParameters
            )
            mapReferenceTypes(context)

            return tripleCollector.buildIterator()
        }
    }

    override fun extendModel(buildParameters: BuildParameters, outputModel: Model) {
        val graph = Graph(buildParameters)

        val graphModel = ModelFactory.createModelForGraph(graph)

        outputModel.add(graphModel)
    }
}