package de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils

import com.sun.jdi.ClassNotLoadedException
import com.sun.jdi.Field

class FieldInfo(private val typeInfoProvider: TypeInfoProvider, val declaringTypeInfo: TypeInfo.ReferenceTypeInfo.CreatedType, val jdiField: Field): HasRCN {
    init {
        if (declaringTypeInfo.jdiType != jdiField.declaringType())
            throw java.lang.IllegalArgumentException("The JDI field instance does not belong to the given declaring type.")
    }

    override val rcn: String = "${declaringTypeInfo.rcn}.${jdiField.name()}"

    val typeInfo: TypeInfo
        get() = try {
                typeInfoProvider.getTypeInfo(jdiField.type())
            } catch (e: ClassNotLoadedException) {
                typeInfoProvider.getNotYetLoadedTypeInfo(jdiField.typeName())
            }
}