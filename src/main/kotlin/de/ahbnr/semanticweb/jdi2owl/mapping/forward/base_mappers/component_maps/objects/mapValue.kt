package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.objects

import com.sun.jdi.*
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.MappingContext
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.Node
import org.apache.jena.graph.NodeFactory

fun mapValue(jdiValue: Value?, context: MappingContext): Node? = with(context) {
    when (jdiValue) {
        is PrimitiveValue -> mapPrimitiveValue(jdiValue, this)
        is ObjectReference -> NodeFactory.createURI(IRIs.run.genObjectIRI(jdiValue))
        // apparently null values are mirrored directly as null:
        // https://docs.oracle.com/en/java/javase/11/docs/api/jdk.jdi/com/sun/jdi/Value.html
        null -> NodeFactory.createURI(IRIs.java.`null`)
        else -> {
            logger.error("Encountered unknown kind of value: ${jdiValue}.")
            null
        }
    }
}

private fun mapPrimitiveValue(value: PrimitiveValue, context: MappingContext): Node? = with(context) {
    when (value) {
        is BooleanValue -> NodeFactory.createLiteralByValue(value.value(), XSDDatatype.XSDboolean)
        is ByteValue -> NodeFactory.createLiteralByValue(value.value(), XSDDatatype.XSDbyte)
        is CharValue -> NodeFactory.createLiteralByValue(
            value.value().code,
            XSDDatatype.XSDunsignedShort
        )
        is DoubleValue -> NodeFactory.createLiteralByValue(value.value(), XSDDatatype.XSDdouble)
        is FloatValue -> NodeFactory.createLiteralByValue(value.value(), XSDDatatype.XSDfloat)
        is IntegerValue -> NodeFactory.createLiteralByValue(value.value(), XSDDatatype.XSDint)
        is LongValue -> NodeFactory.createLiteralByValue(value.value(), XSDDatatype.XSDlong)
        is ShortValue -> NodeFactory.createLiteralByValue(value.value(), XSDDatatype.XSDshort)
        else -> {
            logger.error("Encountered unknown kind of primitive value: $value.")
            null
        }
    }
}