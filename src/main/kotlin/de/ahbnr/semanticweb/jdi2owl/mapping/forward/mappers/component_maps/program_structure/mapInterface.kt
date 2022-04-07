package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.TypeInfo

fun mapInterface(context: InterfaceContext) {
    with(context) {
        if (buildParameters.limiter.canReferenceTypeBeSkipped(typeInfo.jdiType))
            return

        tripleCollector.addStatement(
            typeIRI,
            IRIs.rdf.type,
            IRIs.owl.Class
        )

        // This, as an individual, is a Java Interface
        tripleCollector.addStatement(
            typeIRI,
            IRIs.rdf.type,
            IRIs.java.Interface
        )

        val superInterfaces = typeInfo.jdiType.superinterfaces().filterNot {
            buildParameters.limiter.canReferenceTypeBeSkipped(it)
        }

        if (superInterfaces.isEmpty()) {
            // If an interface has no direct superinterface, then its java.lang.Object is a direct supertype
            // https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.10.2
            tripleCollector.addStatement(
                typeIRI,
                IRIs.rdfs.subClassOf,
                IRIs.prog.java_lang_Object
            )
        } else {
            for (superInterface in superInterfaces) {
                val superInterfaceInfo = buildParameters.typeInfoProvider.getTypeInfo(superInterface)

                tripleCollector.addStatement(
                    typeIRI,
                    IRIs.rdfs.subClassOf,
                    IRIs.prog.genReferenceTypeIRI(superInterfaceInfo)
                )
            }
        }

        mapMethods(this)
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
