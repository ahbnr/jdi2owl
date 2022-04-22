package de.ahbnr.semanticweb.jdi2owl.mapping.forward

import de.ahbnr.semanticweb.jdi2owl.debugging.JvmState
import de.ahbnr.semanticweb.jdi2owl.mapping.MappingLimiter
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TypeInfoProvider

data class BuildParameters(
    val jvmState: JvmState,
    val typeInfoProvider: TypeInfoProvider,
    val limiter: MappingLimiter
)
