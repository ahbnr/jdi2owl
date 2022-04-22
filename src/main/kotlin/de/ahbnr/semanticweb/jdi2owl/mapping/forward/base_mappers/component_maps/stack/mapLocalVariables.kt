package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.stack

import com.sun.jdi.AbsentInformationException

fun mapLocalVariables(context: StackFrameContext) = with(context) {
    val methodInfo = run {
        val jdiMethod = frame.location().method()
        val declaringTypeInfo = buildParameters.typeInfoProvider.getTypeInfo(jdiMethod.declaringType())

        declaringTypeInfo.getMethodInfo(jdiMethod)
    }

    val variables = try {
        frame.visibleVariables()
    } catch (e: AbsentInformationException) {
        logger.debug("Can not load variable information for frame $frameDepth")
        return
    }

    val values = frame.getValues(variables)

    for ((variable, value) in values) {
        val variableInfo = methodInfo.getVariableInfo(variable)

        withVariableValueContext(value, variableInfo) {
            mapLocalVariable(this)
        }
    }
}
