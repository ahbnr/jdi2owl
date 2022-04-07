package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.runtime_components

import com.sun.jdi.Value
import de.ahbnr.semanticweb.jdi2owl.debugging.mirrors.IterableMirror
import de.ahbnr.semanticweb.jdi2owl.debugging.mirrors.utils.MirroringError
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.FieldInfo
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.TypeInfo
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure.FieldContext
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.implementsInterface

fun mapIterable(context: ObjectContext) {
    with(context) {
        val typeInfo = typeInfo
            as? TypeInfo.ReferenceTypeInfo.CreatedType.ClassOrInterface.Class
            ?: return

        if (!typeInfo.jdiType.implementsInterface("java.lang.Iterable"))
            return

        if (buildParameters.limiter.canSequenceBeSkipped(
                `object`,
                referenceContexts
            )
        )
            return

        try {
            val iterable = IterableMirror(`object`, buildParameters.jvmState.pausedThread)

            val iterator = iterable.iterator()
            if (iterator != null) {
                // FIXME: Potentially infinite iterator! We should add a limiter
                val elementList = iterator.asSequence().toList()
                val componentType = elementList.firstOrNull()?.type()

                withSequenceContext(
                    cardinality = elementList.size,
                    componentTypeInfo = componentType?.let { buildParameters.typeInfoProvider.getTypeInfo(it) },
                    elementList
                ) {
                    mapSequence(this)
                }
            } else {
                logger.warning("Can not map elements of iterable $objectIRI because its iterator() method returns null.")
            }
        } catch (e: MirroringError) {
            logger.error(e.message)
        }
    }
}