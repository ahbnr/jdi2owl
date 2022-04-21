package de.ahbnr.semanticweb.jdi2owl.plugins

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.IMapper
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.pluginapi.MapperProvider

class DummyMapperProvider: MapperProvider {
    override fun getMappers(): List<IMapper> = listOf(DummyMapper())
}