package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.TypeInfo

fun mapInterface(context: InterfaceContext) = with(context) {
    with(IRIs) {
        tripleCollector.dsl {
            typeIRI {
                // This, as an individual, is a Java Interface
                rdf.type of java.Interface
            }
        }
    }
}

interface InterfaceContext: CreatedTypeContext {
    override val typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType.ClassOrInterface.Interface
}

fun CreatedTypeContext.withInterfaceContext(
    typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType.ClassOrInterface.Interface,
    block: InterfaceContext.() -> Unit
) {
    object: CreatedTypeContext by this, InterfaceContext {
        override val typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType.ClassOrInterface.Interface = typeInfo
    }.apply(block)
}
