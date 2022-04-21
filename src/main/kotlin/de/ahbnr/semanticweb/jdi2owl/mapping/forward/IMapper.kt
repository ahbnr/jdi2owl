package de.ahbnr.semanticweb.jdi2owl.mapping.forward

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.ClassMapper
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.ObjectMapper
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.StackMapper
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.pluginapi.MapperProvider
import org.apache.jena.rdf.model.Model
import java.util.ServiceLoader

interface IMapper {
    fun extendModel(buildParameters: BuildParameters, outputModel: Model)

    companion object {
        /**
         * Retrieves the default set of IMapper implementations (ClassMapper, ObjectMapper, and StackMapper)
         * and also all implementations on the classpath that are made available via the Java ServiceLoader
         * framework.
         *
         * That is, if you want to dynamically load a mapper plugin, then implement the MapperProvider
         * interface, put the implementation on the classpath, and provide a META-INF/services file for it.
         *
         * See the mapper plugin in plugins/DummyPlugin and the MappingPluginTest for an example of a
         * dynamically loaded mapper extension.
         */
        fun getAllMappers(): List<IMapper> = mutableListOf<IMapper>().apply {
            add( ClassMapper() )
            add( ObjectMapper() )
            add( StackMapper() )

            // Load plugins
            val serviceLoader = ServiceLoader.load(MapperProvider::class.java)
            addAll(serviceLoader.flatMap { it.getMappers() })
        }
    }
}