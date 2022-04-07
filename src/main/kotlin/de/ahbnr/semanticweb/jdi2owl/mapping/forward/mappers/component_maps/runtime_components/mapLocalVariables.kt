package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.runtime_components

import com.sun.jdi.AbsentInformationException
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.MethodInfo

fun mapLocalVariables(context: StackFrameContext) = with(context) {
    val jdiMethod = frame.location().method()
    val methodInfo = MethodInfo(jdiMethod, buildParameters)
    val methodVariableDeclarations = methodInfo.variables

    val variables = try {
        frame.visibleVariables()
    } catch (e: AbsentInformationException) {
        logger.debug("Can not load variable information for frame $frameDepth")
        null
    }

    if (variables != null) {
        val values = frame.getValues(variables)

        for ((variable, value) in values) {
            val variableInfo = methodVariableDeclarations.find { it.jdiLocalVariable == variable }

            if (variableInfo == null) {
                logger.error("Could not retrieve information on a variable declaration for a stack variable.")
                continue
            }

            withVariableValueContext(value, variableInfo) {
                mapLocalVariable(this)
            }
        }
    }
}
