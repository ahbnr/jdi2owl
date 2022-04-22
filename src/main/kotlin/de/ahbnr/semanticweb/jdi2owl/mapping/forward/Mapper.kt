package de.ahbnr.semanticweb.jdi2owl.mapping.forward

import org.apache.jena.rdf.model.Model

interface Mapper {
    fun extendModel(buildParameters: BuildParameters, outputModel: Model)
}