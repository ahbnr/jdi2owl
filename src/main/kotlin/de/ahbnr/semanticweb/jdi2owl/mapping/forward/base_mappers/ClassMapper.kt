package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.program_structure.mapReferenceTypes
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.pluginapi.BaseMappingListener

internal class ClassMapper(
    mappingListeners: Collection<BaseMappingListener>
): ContextBasedMapper( mappingListeners ) {
    override fun map(context: MappingContext) = mapReferenceTypes(context)
}