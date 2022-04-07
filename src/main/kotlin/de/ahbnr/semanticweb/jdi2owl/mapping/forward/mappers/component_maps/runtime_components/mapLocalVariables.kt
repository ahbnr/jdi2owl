package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.runtime_components

import com.sun.jdi.AbsentInformationException
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.MethodInfo

fun mapLocalVariables(context: StackFrameContext) = with(context) {
    val methodInfo = run {
        val jdiMethod = frame.location().method()
        val declaringTypeInfo = buildParameters.typeInfoProvider.getTypeInfo(jdiMethod.declaringType())

        declaringTypeInfo.getMethodInfo(jdiMethod)
    }
    val methodVariableDeclarations = methodInfo.jdiMethod.variables()

    val variables = try {
        frame.visibleVariables()
    } catch (e: AbsentInformationException) {
        logger.debug("Can not load variable information for frame $frameDepth")
        null
    }

    if (variables != null) {
        val values = frame.getValues(variables)

        for ((variable, value) in values) {
            val variableInfo = methodInfo.getVariableInfo(variable)

            withVariableValueContext(value, variableInfo) {
                mapLocalVariable(this)
            }
        }
    }
}
