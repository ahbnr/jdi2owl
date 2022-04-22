package de.ahbnr.semanticweb.jdi2owl.plugins

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.Mapper
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.pluginapi.BaseMappingListener
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.pluginapi.MappingPlugin

class DummyMapperPlugin: MappingPlugin {
    override fun getListeners(): List<BaseMappingListener> = emptyList()
    override fun getMappers(): List<Mapper> = listOf(DummyMapper())
}