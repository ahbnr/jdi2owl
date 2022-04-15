package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.runtime_components

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.contexts.MappingContext
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.ValueToNodeMapper

fun mapStack(context: MappingContext) = with(context) {
    val valueMapper = ValueToNodeMapper()

    withStackContext(valueMapper) {
        val numFrames = buildParameters.jvmState.pausedThread.frameCount()

        for (frameDepth in 0 until numFrames) {
            val frame = buildParameters.jvmState.pausedThread.frame(frameDepth)
            val frameIRI = IRIs.run.genFrameIRI(frameDepth)

            withStackFrameContext(frameDepth, frame, frameIRI) {
                mapStackFrame(this)
            }
        }
    }
}

interface StackMappingContext: MappingContext {
    val valueMapper: ValueToNodeMapper
}

fun <R> MappingContext.withStackContext(
    valueMapper: ValueToNodeMapper,
    block: StackMappingContext.() -> R
): R =
    object: MappingContext by this, StackMappingContext {
        override val valueMapper: ValueToNodeMapper = valueMapper
    }.let(block)
