package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure

import com.sun.jdi.ClassNotLoadedException
import com.sun.jdi.PrimitiveType
import com.sun.jdi.ReferenceType
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.TypeInfo
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.JavaType

fun mapArrayType(context: ArrayTypeContext) {
    with(context) {
        if (buildParameters.limiter.canReferenceTypeBeSkipped(typeInfo.jdiType))
            return

        // this, as an individual, is an array:
        tripleCollector.addStatement(
            typeIRI,
            IRIs.rdf.type,
            IRIs.java.Array
        )

        // Now we need to clarify the type of the array elements
        val componentType = try {
            JavaType.LoadedType(typeInfo.jdiType.componentType())
        } catch (e: ClassNotLoadedException) {
            JavaType.UnloadedType(typeInfo.jdiType.componentTypeName())
        }

        // Arrays are also a class (punning) where all member individuals are
        // members of
        //    the class Object[] if the component type is a reference type
        //    the interfaces Cloneable and Serializable if the component type is a primitive type
        // and some more supertypes, see https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.10.3
        //
        // We define Object[] and the synthetic PrimitiveArray class in the base ontology.
        // There, additional appropriate OWL superclasses like the above interfaces are already associated.
        when (componentType) {
            is JavaType.LoadedType -> {
                when (componentType.type) {
                    is ReferenceType ->
                        tripleCollector.addStatement(
                            typeIRI,
                            IRIs.rdfs.subClassOf,
                            IRIs.prog.`java_lang_Object%5B%5D`
                        )
                    is PrimitiveType ->
                        tripleCollector.addStatement(
                            typeIRI,
                            IRIs.rdfs.subClassOf,
                            IRIs.java.PrimitiveArray
                        )
                    else -> logger.error("Encountered unknown kind of type: ${componentType.type}")
                }
            }

            is JavaType.UnloadedType -> {
                val componentTypeInfo = buildParameters.typeInfoProvider.getNotYetLoadedTypeInfo(componentType.typeName)
                val componentTypeIRI = IRIs.prog.genReferenceTypeIRI(componentTypeInfo)
                withNotYetLoadedTypeContext(componentTypeInfo, componentTypeIRI) {
                    mapNotYetLoadedType(this)
                }

                tripleCollector.addStatement(
                    typeIRI,
                    IRIs.rdfs.subClassOf,
                    IRIs.java.UnloadedTypeArray
                )
            }
        }
    }
}

interface ArrayTypeContext: CreatedTypeContext {
    override val typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType.ArrayType
}

fun CreatedTypeContext.withArrayTypeContext(
    typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType.ArrayType,
    block: ArrayTypeContext.() -> Unit
) {
    object: CreatedTypeContext by this, ArrayTypeContext {
        override val typeInfo: TypeInfo.ReferenceTypeInfo.CreatedType.ArrayType = typeInfo
    }.apply(block)
}
