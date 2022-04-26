package de.ahbnr.semanticweb.jdi2owl.mapping.forward

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.pluginapi.MappingPlugin
import java.util.*

class MappingWithPlugins(
    private val plugins: Collection<MappingPlugin>,
    /**
     * Iff true, it will also load all implementations of MappingPlugin on the classpath that are made available via the
     * Java ServiceLoader framework.
     *
     * That is, if you want to dynamically load a mapper plugin, then implement the MappingPlugin
     * interface, put the implementation on the classpath, and provide a META-INF/services file for it.
     *
     * See the mapper plugin in plugins/DummyPlugin and the MappingPluginTest for an example of a
     * dynamically loaded mapper extension.
     */
    private val dynamicallyLoadPlugins: Boolean = false,
    /**
     * if true, run the plugin mappers only
     */
    private val disableBaseMapping: Boolean = false
): MappingMode() {
    override fun getMappers(): List<Mapper> {
        val allPlugins = plugins.toMutableList()
        if (dynamicallyLoadPlugins)
            allPlugins.addAll(ServiceLoader.load(MappingPlugin::class.java))

        val listeners = allPlugins.flatMap { it.getListeners() }
        val pluginMappers = allPlugins.flatMap { it.getMappers() }

        return if (disableBaseMapping)
            pluginMappers
        else
            getBaseMappers(listeners) + pluginMappers
    }
}