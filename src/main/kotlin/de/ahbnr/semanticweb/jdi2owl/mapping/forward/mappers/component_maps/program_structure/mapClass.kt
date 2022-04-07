package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure

import com.sun.jdi.ClassType
import de.ahbnr.semanticweb.jdi2owl.mapping.datatypes.JavaAccessModifierDatatype
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.TypeInfo

fun mapClass(context: ClassContext) {
    context.apply {
        if (buildParameters.limiter.canReferenceTypeBeSkipped(typeInfo.jdiType))
            return

        // classSubject is an owl class
        // FIXME: Shouldnt this be implied by being a subClassOf java.lang.Object?
        //   either it is not, or some reasoners / frameworks do not recognize this implication because some
        //   things dont work when this is removed.
        //   For example: The SyntacticLocalityModuleExtractor does not extract OWL class definitions for
        //   the java classes.
        tripleCollector.addStatement(
            typeIRI,
            IRIs.rdf.type,
            IRIs.owl.Class
        )

        // This, as an individual, is a Java Class
        tripleCollector.addStatement(
            typeIRI,
            IRIs.rdf.type,
            IRIs.java.Class
        )

        // But we use Punning, and it is also an OWL class
        // More specifically, all its individuals are also part of the superclass
        //
        // (btw. prog:java.lang.Object is defined as an OWL class in the base ontology)
        val superClass: ClassType? = typeInfo.jdiType.superclass()
        if (superClass != null && !buildParameters.limiter.canReferenceTypeBeSkipped(superClass)) {
            val superClassInfo = buildParameters.typeInfoProvider.getTypeInfo(superClass)

            tripleCollector.addStatement(
                typeIRI,
                IRIs.rdfs.subClassOf,
                IRIs.prog.genReferenceTypeURI(superClassInfo)
            )
        } else if (typeInfo.jdiType.name() != "java.lang.Object") {
            tripleCollector.addStatement(
                typeIRI,
                IRIs.rdfs.subClassOf,
                IRIs.prog.java_lang_Object
            )
        }

        // https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.10.2
        val superInterfaces =
            typeInfo.jdiType.interfaces().filterNot { buildParameters.limiter.canReferenceTypeBeSkipped(it) }
        for (superInterface in superInterfaces) {
            val superInterfaceInfo = buildParameters.typeInfoProvider.getTypeInfo(superInterface)

            tripleCollector.addStatement(
                typeIRI,
                IRIs.rdfs.subClassOf,
                IRIs.prog.genReferenceTypeURI(superInterfaceInfo)
            )
        }

        // FIXME: why do Kamburjan et. al. use subClassOf and prog:Object here?
        //  Also: Classes are also objects in Java. However, I moved this to the object mapper
        // tripleCollector.addStatement(
        //     classSubject,
        //     URIs.rdfs.subClassOf,
        //     URIs.prog.Object
        // )

        // Define accessibility
        tripleCollector.addStatement(
            typeIRI,
            IRIs.java.hasAccessModifier,
            JavaAccessModifierDatatype
                .AccessModifierLiteral
                .fromJdiAccessible(typeInfo.jdiType)
                .toNode()
        )

        mapMethods(this)
        mapFields(this)
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
