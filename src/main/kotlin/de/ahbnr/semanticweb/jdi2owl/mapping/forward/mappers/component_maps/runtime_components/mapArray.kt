package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.runtime_components

import com.sun.jdi.*
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TypeInfo

fun mapArray(context: ObjectContext) = with(context) {
    val `object` = `object`
        as? ArrayReference
        ?: return

    val typeInfo = typeInfo
        as? TypeInfo.ReferenceTypeInfo.CreatedType.ArrayType
        ?: run {
            logger.error("Encountered array whose type is not an array type: Object $objectIRI of type $typeIRI.")
            return
        }

    if (buildParameters.limiter.canSequenceBeSkipped(
            `object`,
            referenceContexts
        )
    )
        return

    withSequenceContext(
        cardinality = `object`.length(),
        componentTypeInfo = typeInfo.componentType,
        elements = `object`.values
    ) {
        mapSequence(this)
    }
}

