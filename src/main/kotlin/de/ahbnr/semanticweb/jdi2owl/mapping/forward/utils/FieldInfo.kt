package de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils

import com.sun.jdi.Field

class FieldInfo(val typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType, val jdiField: Field): HasRCN {
    init {
        if (typeInfo.jdiType != jdiField.declaringType())
            throw java.lang.IllegalArgumentException("The JDI field instance does not belong to the given declaring type.")
    }

    override val rcn: String = "${typeInfo.rcn}.${jdiField.name()}"
}