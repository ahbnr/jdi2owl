package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.stack

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.MappingContext

fun mapStack(context: MappingContext) = with(context) {
    val numFrames = buildParameters.jvmState.pausedThread.frameCount()

    for (frameDepth in 0 until numFrames) {
        val frame = buildParameters.jvmState.pausedThread.frame(frameDepth)
        val frameIRI = IRIs.run.genFrameIRI(frameDepth)

        withStackFrameContext(frameDepth, frame, frameIRI) {
            mapStackFrame(this)
        }
    }
}