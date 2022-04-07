package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.runtime_components

import com.sun.jdi.StackFrame
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.contexts.MappingContext
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.ValueToNodeMapper
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.NodeFactory

fun mapStackFrame(context: StackFrameContext) = with(context) {
    tripleCollector.addStatement(
        frameIRI,
        IRIs.rdf.type,
        IRIs.owl.NamedIndividual
    )

    // this *is* a stack frame
    tripleCollector.addStatement(
        frameIRI,
        IRIs.rdf.type,
        IRIs.java.StackFrame
    )

    // ...and it is at a certain depth
    tripleCollector.addStatement(
        frameIRI,
        IRIs.java.isAtStackDepth,
        NodeFactory.createLiteralByValue(frameDepth, XSDDatatype.XSDint)
    )

    // ...and it oftentimes has a `this` reference:
    val thisRef = frame.thisObject()
    if (thisRef != null) {
        val thisObjectNode = valueMapper.map(thisRef)

        if (thisObjectNode != null) {
            tripleCollector.addStatement(
                frameIRI,
                IRIs.java.`this`,
                thisObjectNode
            )

            // For the current frame, we create a quick alias for the this object
            if (frameDepth == 0) {
                tripleCollector.addStatement(
                    IRIs.local.`this`,
                    IRIs.rdf.type,
                    IRIs.owl.NamedIndividual
                )

                tripleCollector.addStatement(
                    IRIs.local.`this`,
                    IRIs.owl.sameAs,
                    thisObjectNode
                )
            }
        } else {
            logger.error("Could not find `this` object for frame. This should never happen.")
        }
    }

    // ...and it holds some variables
    mapLocalVariables(this)
}

interface StackFrameContext: StackMappingContext {
    val frameDepth: Int
    // Careful, no invokeMethod calls should take place from here on to keep this frame reference
    // valid.
    val frame: StackFrame
    val frameIRI: String
}

fun <R> StackMappingContext.withStackFrameContext(
    frameDepth: Int,
    frame: StackFrame,
    frameIRI: String,
    block: StackFrameContext.() -> R
): R =
    object: StackMappingContext by this, StackFrameContext {
        override val frameDepth: Int = frameDepth
        override val frame: StackFrame = frame
        override val frameIRI: String = frameIRI
    }.let(block)
