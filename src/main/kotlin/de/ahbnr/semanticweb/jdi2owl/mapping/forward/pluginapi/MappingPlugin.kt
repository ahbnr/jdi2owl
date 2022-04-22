package de.ahbnr.semanticweb.jdi2owl.mapping.forward.pluginapi

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.Mapper

/**
 * Interface for mapper plugins, see also the MappingWithPlugins class
 */
interface MappingPlugin {
    /**
     * If you do not want to create a mapping plugin completely from scratch, then implement a BaseMappingListener and
     * supply it here.
     *
     * The advantage of listeners is, that they are called by the base mapping for specific parts of the program state
     * (e.g. a reference type, method, ...), so you dont have to implement a traversal algorithm for the JVM state.
     */
    fun getListeners(): List<BaseMappingListener>

    /**
     * Implement the Mapper interface and supply instances here to create a mapping completely from scratch
     */
    fun getMappers(): List<Mapper>
}