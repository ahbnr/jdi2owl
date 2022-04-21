package de.ahbnr.semanticweb.jdi2owl.mapping.forward.pluginapi

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.IMapper

/**
 * Interface for loading mapper plugins, see IMapper.getAllMappers()
 */
interface MapperProvider {
    fun getMappers(): List<IMapper>
}