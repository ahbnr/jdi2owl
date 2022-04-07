package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.runtime_components

import com.sun.jdi.ClassType

fun mapStaticClassMembers(context: ObjectMappingContext) = with(context) {
    // FIXME: Aren't class type instances already handled by the JvmObjectIterator in the allObjects method?
    val classTypes =
        buildParameters.jvmState.pausedThread.virtualMachine().allClasses().filterIsInstance<ClassType>()

    for (classType in classTypes) {
        if (!classType.isPrepared)
            continue // skip those class types which have not been fully prepared in the vm state yet

        val typeInfo = buildParameters.typeInfoProvider.getTypeInfo(classType)
        val typeIRI = IRIs.prog.genReferenceTypeIRI(typeInfo)

        val fieldValues = classType.getValues(classType.fields().filter { it.isStatic })

        for ((field, value) in fieldValues) {
            val fieldInfo = typeInfo.getFieldInfo(field)

            withFieldValueContext(
                value,
                fieldReceiverIRI = typeIRI,
                fieldInfo = fieldInfo,
                fieldIRI = IRIs.prog.genFieldIRI(fieldInfo)
            ) {
                mapField(this)
            }
        }
    }
}