package de.ahbnr.semanticweb.jdi2owl.mapping.forward.pluginapi

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.MappingContext

interface BaseMappingListener {
    fun mapInContext(mappingContext: MappingContext)
}