package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure

import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.NodeFactory

fun mapMethodLocation(context: MethodContext) {
    with(context) {
        // add declaration location
        val declarationLocation = methodInfo.declarationLocation
        if (declarationLocation != null) {
            val locationURI = IRIs.prog.genLocationURI(declarationLocation)

            // it *is* a java:Location
            tripleCollector.addStatement(
                locationURI,
                IRIs.rdf.type,
                IRIs.java.Location
            )

            // its a location of a method
            tripleCollector.addStatement(
                methodIRI,
                IRIs.java.isDeclaredAt,
                locationURI
            )

            // set source path
            tripleCollector.addStatement(
                locationURI,
                IRIs.java.isAtSourcePath,
                NodeFactory.createLiteralByValue(
                    declarationLocation.sourcePath,
                    XSDDatatype.XSDstring
                )
            )

            // set line
            tripleCollector.addStatement(
                locationURI,
                IRIs.java.isAtLine,
                NodeFactory.createLiteralByValue(
                    declarationLocation.line,
                    XSDDatatype.XSDint
                )
            )
        }

        // add body definition location
        val definitionLocation = methodInfo.definitionLocation
        if (definitionLocation != null) {
            val locationURI = IRIs.prog.genLocationURI(definitionLocation)

            // it *is* a java:Location
            tripleCollector.addStatement(
                locationURI,
                IRIs.rdf.type,
                IRIs.java.Location
            )

            tripleCollector.addStatement(
                methodIRI,
                IRIs.java.isDefinedAt,
                locationURI
            )

            // set source path
            tripleCollector.addStatement(
                locationURI,
                IRIs.java.isAtSourcePath,
                NodeFactory.createLiteralByValue(definitionLocation.sourcePath, XSDDatatype.XSDstring)
            )

            // set line
            tripleCollector.addStatement(
                locationURI,
                IRIs.java.isAtLine,
                NodeFactory.createLiteralByValue(definitionLocation.line, XSDDatatype.XSDint)
            )
        }
    }
}