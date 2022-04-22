package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.program_structure

import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.NodeFactory

fun mapMethodLocation(context: MethodContext) = with(context) {
    // add body definition location
    val definitionLocation = methodInfo.definitionLocation
    if (definitionLocation != null) {
        with (IRIs) {
            val locationIRI = prog.genLocationIRI(definitionLocation)

            tripleCollector.dsl {
                // it *is* a java:Location
                locationIRI {
                    rdf.type of java.Location

                    // set source path
                    java.isAtSourcePath of NodeFactory.createLiteralByValue(
                        definitionLocation.sourcePath, XSDDatatype.XSDstring
                    )

                    // set line
                    java.isAtLine of NodeFactory.createLiteralByValue(
                        definitionLocation.line, XSDDatatype.XSDint
                    )
                }

                methodIRI {
                    java.isDefinedAt of locationIRI
                }
            }
        }
    }
}