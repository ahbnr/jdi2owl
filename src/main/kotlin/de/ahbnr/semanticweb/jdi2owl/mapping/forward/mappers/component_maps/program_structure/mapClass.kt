package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure

import de.ahbnr.semanticweb.jdi2owl.mapping.datatypes.JavaAccessModifierDatatype
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.TypeInfo

fun mapClass(context: ClassContext) = with(context) {
    with (IRIs) {
        tripleCollector.dsl {
            typeIRI {
                // this type is a Java class
                rdf.type of java.Class

                // Define accessibility
                java.hasAccessModifier of JavaAccessModifierDatatype
                    .AccessModifierLiteral
                    .fromJdiAccessible(typeInfo.jdiType)
                    .toNode()
            }
        }
    }
}

interface ClassContext: CreatedTypeContext {
    override val typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType.ClassOrInterface.Class
}

fun CreatedTypeContext.withClassContext(
    typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType.ClassOrInterface.Class,
    block: ClassContext.() -> Unit
) {
    object: CreatedTypeContext by this, ClassContext {
        override val typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType.ClassOrInterface.Class = typeInfo
    }.apply(block)
}
