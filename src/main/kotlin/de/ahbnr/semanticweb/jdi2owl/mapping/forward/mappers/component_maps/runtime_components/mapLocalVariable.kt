package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.runtime_components

import com.sun.jdi.Value
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.LocalVariableInfo

fun mapLocalVariable(context: VariableValueContext) = with(context) {
    // we model this via the variable declaration property.
    // The property value depends on the kind of value we have here
    val valueObject = valueMapper.map(value)

    if (valueObject != null) {
        tripleCollector.addStatement(
            frameIRI,
            IRIs.prog.genVariableDeclarationIRI(variableInfo),
            valueObject
        )

        // The values of the current stackframe get special, easily accessible names
        if (frameDepth == 0) {
            val localVarIRI = IRIs.local.genLocalVariableIRI(variableInfo)
            tripleCollector.addStatement(
                localVarIRI,
                IRIs.rdf.type,
                IRIs.owl.NamedIndividual
            )

            // FIXME: This declares even data values as the same as a local variable.
            //   Is this valid OWL 2?
            //   Probably not, 'SameIndividual( local:x "1"^^xsd:int )' fails with a parsing error
            tripleCollector.addStatement(
                localVarIRI,
                IRIs.owl.sameAs,
                valueObject
            )
        }
    }
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
