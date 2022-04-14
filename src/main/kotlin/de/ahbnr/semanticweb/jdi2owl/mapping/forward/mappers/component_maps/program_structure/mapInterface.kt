package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.TypeInfo

fun mapInterface(context: InterfaceContext) = with(context) {
    with(IRIs) {
        tripleCollector.dsl {
            typeIRI {
                // This, as an individual, is a Java Interface
                rdf.type of java.Interface

                // java.lang.Object is a supertype of all interfaces
                // https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.10.2
                rdfs.subClassOf of prog.java_lang_Object
            }
        }
    }

    val superInterfaces = typeInfo.jdiType.superinterfaces().filterNot {
        buildParameters.limiter.canReferenceTypeBeSkipped(it)
    }

    for (superInterface in superInterfaces) {
        val superInterfaceInfo = buildParameters.typeInfoProvider.getTypeInfo(superInterface)

        tripleCollector.addStatement(
            typeIRI,
            IRIs.rdfs.subClassOf,
            IRIs.prog.genReferenceTypeIRI(superInterfaceInfo)
        )
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
