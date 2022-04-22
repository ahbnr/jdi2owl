package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.stack

import com.sun.jdi.ObjectReference
import com.sun.jdi.Value
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.objects.mapValue
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.LocalVariableInfo

fun mapLocalVariable(context: VariableValueContext) = with(context) {
    val valueNode = mapValue(value, this) ?: return

    tripleCollector.addStatement(
        frameIRI,
        IRIs.prog.genVariableDeclarationIRI(variableInfo),
        valueNode
    )

    // The values of the current stackframe get special, easily accessible names
    // FIXME: This creates aliases only for object references... not sure yet how to map primitive values
    if (frameDepth == 0 && (value is ObjectReference || value == null)) {
        val localVarIRI = IRIs.local.genLocalVariableIRI(variableInfo)
        tripleCollector.addStatement(
            localVarIRI,
            IRIs.rdf.type,
            IRIs.owl.NamedIndividual
        )

        tripleCollector.addStatement(
            localVarIRI,
            IRIs.owl.sameAs,
            valueNode
        )
    }

    pluginListeners.mapInContext(this)
}

interface VariableValueContext: StackFrameContext {
    val value: Value?
    val variableInfo: LocalVariableInfo
}

fun StackFrameContext.withVariableValueContext(
    value: Value?,
    variableInfo: LocalVariableInfo,
    block: VariableValueContext.() -> Unit
) {
    object: StackFrameContext by this, VariableValueContext {
        override val value: Value? = value
        override val variableInfo: LocalVariableInfo = variableInfo
    }.apply(block)
}
