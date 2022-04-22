@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE", "JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE")

package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.stack.mapStack
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.pluginapi.BaseMappingListener

internal class StackMapper(
    mappingListeners: Collection<BaseMappingListener>
): ContextBasedMapper( mappingListeners ) {
    override fun map(context: MappingContext) = mapStack(context)
}