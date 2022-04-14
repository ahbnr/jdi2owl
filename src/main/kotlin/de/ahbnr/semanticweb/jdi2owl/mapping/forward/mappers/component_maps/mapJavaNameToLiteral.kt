package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps

import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.Node
import org.apache.jena.graph.NodeFactory

/**
 * Implementation of (⋅)ˢᵗʳ
 */
fun mapJavaNameToLiteral(javaName: String): Node =
    NodeFactory.createLiteralByValue(
        // Remove code point U+0000 that may in theory appear in Java identifiers, but
        // which is not allowed in xsd:strings
        javaName
            .codePoints()
            .filter { it != 0 }
            .collect(::StringBuilder, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString(),
        XSDDatatype.XSDstring
    )
