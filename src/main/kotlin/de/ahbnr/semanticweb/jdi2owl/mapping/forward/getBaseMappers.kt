package de.ahbnr.semanticweb.jdi2owl.mapping.forward

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.ClassMapper
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.ObjectMapper
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.StackMapper
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.pluginapi.BaseMappingListener

internal fun getBaseMappers(mappingListeners: Collection<BaseMappingListener> = emptyList()) = listOf<Mapper>(
    ClassMapper(mappingListeners),
    ObjectMapper(mappingListeners),
    StackMapper(mappingListeners)
)