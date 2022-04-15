package de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils

import com.sun.jdi.ClassNotLoadedException
import com.sun.jdi.LocalVariable
import de.ahbnr.semanticweb.jdi2owl.debugging.utils.InternalJDIUtils

data class LocalVariableInfo(
    private val typeInfoProvider: TypeInfoProvider,
    val jdiLocalVariable: LocalVariable,
    val methodInfo: MethodInfo
): HasRCN {
    // Name that is unique in the context of the method of this variable
    val localName: String
    init {
        val variablesWithSameName = methodInfo.jdiMethod.variablesByName(jdiLocalVariable.name())

        localName = if (variablesWithSameName.size == 1) {
            jdiLocalVariable.name()
        }

        else {
            // If there are multiple variables with the same name in different blocks, we have to differentiate them
            // by the code index of the start of their scope
            val scopeStartCodeIndex = InternalJDIUtils.getScopeStart(jdiLocalVariable).codeIndex()
            "${jdiLocalVariable.name()}-$scopeStartCodeIndex"
        }
    }

    override val rcn: String = "${methodInfo.rcn}.$localName"

    val typeInfo: TypeInfo
        get() = try {
                typeInfoProvider.getTypeInfo(jdiLocalVariable.type())
            }

            catch (e: ClassNotLoadedException) {
                typeInfoProvider.getNotYetLoadedTypeInfo(jdiLocalVariable.typeName())
            }

    fun getLine(): Int? {
        val jdiInfo = InternalJDIUtils.getScopeStart(jdiLocalVariable).lineNumber()

        return if (jdiInfo >= 0)
            jdiInfo
        else null
    }
}