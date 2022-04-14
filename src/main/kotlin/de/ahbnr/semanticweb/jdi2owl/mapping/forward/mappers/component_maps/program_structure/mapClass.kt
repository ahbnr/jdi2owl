package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure

import com.sun.jdi.ClassType
import de.ahbnr.semanticweb.jdi2owl.mapping.datatypes.JavaAccessModifierDatatype
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.TypeInfo

fun mapClass(context: ClassContext) = with(context) {
    tripleCollector.addStatement(
        // this type is a Java class
        typeIRI, IRIs.rdf.type, IRIs.java.Class
    )

    // But we use Punning, and it is also an OWL class
    // All its individuals are also part of the superclass
    val superClass: ClassType? = typeInfo.jdiType.superclass()

    if (superClass != null && !buildParameters.limiter.canReferenceTypeBeSkipped(superClass)) {
        val superClassInfo = buildParameters.typeInfoProvider.getTypeInfo(superClass)

        tripleCollector.addStatement(
            typeIRI,
            IRIs.rdfs.subClassOf,
            IRIs.prog.genReferenceTypeIRI(superClassInfo)
        )
    } // We do not need to handle the case, that there is no superclass. The JLS/JDI allows this only for Object

    // Technically, we do not need to explicitly specify for every class that it is subsumed by Object,
    // because this can be transitively inferred from the superclass.
    //
    // Still, it is useful to do so for query engines that have no inferencing capabilities (e.g. plain SPARQL)
    // Also, this way, we retain Object as a supertype even if the chain of supertypes is interrupted by the mapping
    // limiter
    tripleCollector.addStatement(
        typeIRI,
        IRIs.rdfs.subClassOf,
        IRIs.prog.java_lang_Object
    )

    // We must handle superinterfaces similarly
    // https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.10.2
    val superInterfaces =
        typeInfo.jdiType.interfaces().filterNot { buildParameters.limiter.canReferenceTypeBeSkipped(it) }
    for (superInterface in superInterfaces) {
        val superInterfaceInfo = buildParameters.typeInfoProvider.getTypeInfo(superInterface)

        tripleCollector.addStatement(
            typeIRI,
            IRIs.rdfs.subClassOf,
            IRIs.prog.genReferenceTypeIRI(superInterfaceInfo)
        )
    }

    // Define accessibility
    tripleCollector.addStatement(
        typeIRI,
        IRIs.java.hasAccessModifier,
        JavaAccessModifierDatatype
            .AccessModifierLiteral
            .fromJdiAccessible(typeInfo.jdiType)
            .toNode()
    )
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
