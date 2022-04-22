package de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils

import com.sun.jdi.ClassNotLoadedException
import com.sun.jdi.LocalVariable
import com.sun.jdi.Method
import de.ahbnr.semanticweb.jdi2owl.debugging.utils.InternalJDIUtils

class MethodInfo(
    val typeInfoProvider: TypeInfoProvider,
    val jdiMethod: Method
): HasRCN {
    override val rcn: String

    init {
        val declaringTypeInfo = typeInfoProvider.getTypeInfo(jdiMethod.declaringType())

        // we have to encode the return type to deal with bridge methods which can violate the overloading rules
        // TODO: Mention this in report
        val returnTypeInfo = try {
            typeInfoProvider.getTypeInfo(jdiMethod.returnType())
        } catch (e: ClassNotLoadedException) {
            typeInfoProvider.getNotYetLoadedTypeInfo(jdiMethod.returnTypeName())
        }

        // The JDI either lets us access all argument types if all have been created, or none at all, if at least one
        // of them has not been created.
        // In that case, only the binary names of all argument types are available.
        //
        // However, since we need the correct RCN for each one of them, we can not just work with the binary names.
        // Hence, we access internal APIs to access argument types individually, if they have been created.
        val numArgs = InternalJDIUtils.Method_argumentSignatures(jdiMethod).size
        val argTypeNames = jdiMethod.argumentTypeNames()
        val argumentRCNs = (0 until numArgs).map { i ->
            try {
                typeInfoProvider
                    .getTypeInfo(InternalJDIUtils.Method_argumentType(jdiMethod, i))
                    .rcn
            }

            catch (e: ClassNotLoadedException) {
                typeInfoProvider
                    .getNotYetLoadedTypeInfo(argTypeNames[i])
                    .rcn
            }
        }

        // Btw: For constructors the return type will be "void" and the name will be "<init>"
        rcn = "${declaringTypeInfo.rcn}.-${returnTypeInfo.rcn}-${jdiMethod.name()}(${argumentRCNs.joinToString(",")})"
    }


    val definitionLocation: LocationInfo?
        get() {
            val jdiLocation = jdiMethod.location()
            return if (jdiLocation != null) {
                LocationInfo.fromJdiLocation(jdiLocation)
            } else null
        }

    fun getVariableInfo(jdiVariable: LocalVariable) =
        LocalVariableInfo(typeInfoProvider, jdiVariable, this)
}