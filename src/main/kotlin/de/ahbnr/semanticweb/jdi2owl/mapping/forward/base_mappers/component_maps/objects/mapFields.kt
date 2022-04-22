package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.objects

import com.sun.jdi.Value
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.FieldInfo

fun mapFields(context: ObjectContext) = with(context) {
    val fieldValues =
        `object`.getValues(typeInfo.jdiType.allFields()) // allFields does capture the fields of superclasses

    for ((field, value) in fieldValues) {
        if (field.isStatic) // Static fields are handled by addStaticClassMembers
            continue

        val declaringTypeInfo = buildParameters.typeInfoProvider.getTypeInfo(field.declaringType())
        val fieldInfo = declaringTypeInfo.getFieldInfo(field)
        val fieldIRI = IRIs.prog.genFieldIRI(fieldInfo)

        withFieldValueContext(value, fieldReceiverIRI = objectIRI, fieldInfo, fieldIRI) {
            mapField(this)
        }
    }
}

fun mapField(context: FieldValueContext) = with(context) {
    if (buildParameters.limiter.canFieldBeSkipped(fieldInfo.jdiField))
        return

    // we model a field as an instance of the field property of the class.
    // That one is created by the ClassMapper

    // let's find out the object name, i.e. the name of the field value in case of a reference type value,
    // or the value itself, in case of a primitive value
    val valueNode = mapValue(value, this) ?: return

    tripleCollector.addStatement(
        fieldReceiverIRI,
        fieldIRI,
        valueNode
    )

    pluginListeners.mapInContext(this)
}

interface FieldValueContext: ObjectMappingContext {
    val value: Value?

    // IRI of the object or class for which the field in question stores the value
    val fieldReceiverIRI: String

    val fieldInfo: FieldInfo
    val fieldIRI: String
}

fun ObjectMappingContext.withFieldValueContext(
    value: Value?,
    fieldReceiverIRI: String,
    fieldInfo: FieldInfo,
    fieldIRI: String,
    block: FieldValueContext.() -> Unit
) {
    object: ObjectMappingContext by this, FieldValueContext {
        override val value: Value? = value
        override val fieldReceiverIRI: String = fieldReceiverIRI
        override val fieldInfo: FieldInfo = fieldInfo
        override val fieldIRI: String = fieldIRI
    }.apply(block)
}
