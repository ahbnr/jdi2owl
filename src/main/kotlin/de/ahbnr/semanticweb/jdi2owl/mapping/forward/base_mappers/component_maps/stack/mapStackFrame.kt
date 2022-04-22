package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.stack

import com.sun.jdi.StackFrame
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.MappingContext
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.objects.mapValue
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.NodeFactory

fun mapStackFrame(context: StackFrameContext) = with(context) {
    with(IRIs) {
        tripleCollector.dsl {
            frameIRI {
                rdf.type of owl.NamedIndividual
                rdf.type of java.StackFrame
                java.isAtStackDepth of NodeFactory.createLiteralByValue(frameDepth, XSDDatatype.XSDint)
            }
        }
    }

    // Mapping this reference, if it is present
    val thisRef = frame.thisObject()
    if (thisRef != null) {
        val thisObjectNode = mapValue(thisRef, this)

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

    pluginListeners.mapInContext(this)
}

interface StackFrameContext: MappingContext {
    val frameDepth: Int
    // Careful, no invokeMethod calls should take place from here on to keep this frame reference
    // valid.
    val frame: StackFrame
    val frameIRI: String
}

fun <R> MappingContext.withStackFrameContext(
    frameDepth: Int,
    frame: StackFrame,
    frameIRI: String,
    block: StackFrameContext.() -> R
): R =
    object: MappingContext by this, StackFrameContext {
        override val frameDepth: Int = frameDepth
        override val frame: StackFrame = frame
        override val frameIRI: String = frameIRI
    }.let(block)
