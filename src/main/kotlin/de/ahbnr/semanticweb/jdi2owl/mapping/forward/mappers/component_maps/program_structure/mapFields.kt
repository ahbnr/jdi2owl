package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure

import com.sun.jdi.ClassNotLoadedException
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.FieldInfo
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.TypeInfo
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.utils.addReferenceOrNullClass
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.JavaType
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TripleCollector
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.NodeFactory

fun mapFields(context: CreatedTypeContext) {
    with(context) {
        // Note, that "fields()" gives us the fields of the type in question only, not the fields of superclasses
        for (field in typeInfo.jdiType.fields()) {
            val fieldInfo = typeInfo.getFieldInfo(field)
            // A field is a property (of a class instance).
            // Hence, we model it as a property in the ontology
            val fieldIRI = IRIs.prog.genFieldIRI(fieldInfo)

            withFieldContext(fieldInfo, fieldIRI) {
                mapField(this)
            }
        }
    }
}

fun mapField(context: FieldContext) {
    context.apply {
        if (buildParameters.limiter.canFieldBeSkipped(fieldInfo.jdiField))
            return

        // this field is a java field
        tripleCollector.addStatement(
            fieldIRI,
            IRIs.rdf.type,
            IRIs.java.Field
        )

        // and it is part of the class
        tripleCollector.addStatement(
            typeIRI,
            IRIs.java.hasField,
            fieldIRI
        )

        // Now we need to clarify the type of the field
        val fieldType = try {
            JavaType.LoadedType(fieldInfo.jdiField.type())
        } catch (e: ClassNotLoadedException) {
            JavaType.UnloadedType(fieldInfo.jdiField.typeName())
        }

        // Fields are modeled as properties.
        // This way, the field type can be encoded in the property range.
        // The exact kind of property and ranges now depend on the field type:
        when (fieldType) {
            is JavaType.LoadedType -> when (
                val fieldTypeInfo = buildParameters.typeInfoProvider.getTypeInfo(fieldType.type)
            ) {
                // "normal" Java classes
                is TypeInfo.ReferenceTypeInfo.CreatedType -> {
                    // Since the Java field is of a class type here, it must be an ObjectProperty,
                    // (https://www.w3.org/TR/owl-ref/#ObjectProperty-def)
                    // that is, a property that links individuals to individuals
                    // (here: Java class instances (parent object) to Java class instances (field value))
                    tripleCollector.addStatement(
                        fieldIRI,
                        IRIs.rdf.type,
                        IRIs.owl.ObjectProperty
                    )

                    // We now restrict the kind of values the field property can link to, that is, we
                    // set the rdfs:range to the field type
                    tripleCollector.addStatement(
                        fieldIRI,
                        IRIs.rdfs.range,
                        withCreatedTypeContext( fieldTypeInfo, IRIs.prog.genReferenceTypeIRI(fieldTypeInfo) ) {
                            addReferenceOrNullClass(this)
                        }
                    )

                    if (!fieldInfo.jdiField.isStatic) {
                        // We force all individuals of the class to implement the field
                        // FIXME: This creates a lot of subClass axioms. Inefficient.
                        //   It is a form of closure axiom. Should we really enforce this?
                        //   It does create reasoning overhead
                        // tripleCollector.addStatement(
                        //     classURI,
                        //     URIs.rdfs.subClassOf,
                        //     tripleCollector.addConstruct(
                        //         TripleCollector.BlankNodeConstruct.OWLSome(
                        //             fieldURI,
                        //             addReferenceOrNullClass(URIs.prog.genReferenceTypeURI(fieldType.type))
                        //         )
                        //     )
                        // )
                    }
                }

                is TypeInfo.PrimitiveTypeInfo -> {
                    tripleCollector.addStatement(
                        fieldIRI,
                        IRIs.rdf.type,
                        IRIs.owl.DatatypeProperty
                    )

                    val datatypeIRI = IRIs.java.genPrimitiveTypeURI(fieldTypeInfo)
                    if (datatypeIRI == null) {
                        logger.error("Unknown primitive data type: ${fieldType.type}")
                        return
                    }

                    tripleCollector.addStatement(
                        fieldIRI,
                        IRIs.rdfs.range,
                        datatypeIRI
                    )

                    if (!fieldInfo.jdiField.isStatic) {
                        // We force all individuals of the class to implement these fields
                        // FIXME: This creates a lot of subClass axioms. Inefficient.
                        //   It is a form of closure axiom. Should we really enforce this?
                        //   It does create reasoning overhead
                        // tripleCollector.addStatement(
                        //     classURI,
                        //     URIs.rdfs.subClassOf,
                        //     tripleCollector.addConstruct(
                        //         TripleCollector.BlankNodeConstruct.OWLSome(
                        //             fieldURI,
                        //             NodeFactory.createURI(datatypeURI)
                        //         )
                        //     )
                        // )
                    }
                }
                else -> {
                    logger.error("Encountered unknown kind of type: ${fieldType.type}.")
                }
            }

            is JavaType.UnloadedType -> {
                val fieldTypeInfo = buildParameters.typeInfoProvider.getNotYetLoadedTypeInfo(fieldType.typeName)
                val fieldTypeIRI = IRIs.prog.genReferenceTypeIRI(fieldTypeInfo)

                withNotYetLoadedTypeContext(
                    fieldTypeInfo,
                    fieldTypeIRI
                ) {
                    mapNotYetLoadedType(this)
                }

                tripleCollector.addStatement(
                    fieldIRI,
                    IRIs.rdf.type,
                    IRIs.owl.ObjectProperty
                )

                tripleCollector.addStatement(
                    fieldIRI,
                    IRIs.rdfs.range,
                    withNotYetLoadedTypeContext(fieldTypeInfo, fieldTypeIRI) {
                        addReferenceOrNullClass(this)
                    }
                )
            }
        }

        tripleCollector.addStatement(
            fieldIRI,
            IRIs.java.isStatic,
            NodeFactory.createLiteralByValue(fieldInfo.jdiField.isStatic, XSDDatatype.XSDboolean)
        )

        if (fieldInfo.jdiField.isStatic) {
            // static field instances are defined for the class itself, not the instances
            tripleCollector.addStatement(
                fieldIRI,
                IRIs.rdfs.domain,
                // Punning: We treat the class as an individual
                tripleCollector.addConstruct(
                    TripleCollector.BlankNodeConstruct.OWLOneOf.fromIRIs(
                        listOf( typeIRI )
                    )
                )
            )
        } else {
            // an instance field is a thing defined for every object instance of the class concept via rdfs:domain
            // (this also drives some implications, e.g. if there exists a field, there must also be some class it belongs to etc)
            tripleCollector.addStatement(
                fieldIRI,
                IRIs.rdfs.domain,
                typeIRI
            )
        }

        // Fields are functional properties.
        // For every instance of the field (there is only one for the object) there is exactly one property value
        tripleCollector.addStatement(
            fieldIRI,
            IRIs.rdf.type,
            IRIs.owl.FunctionalProperty
        )
    }
}
interface FieldContext: CreatedTypeContext {
    val fieldInfo: FieldInfo
    val fieldIRI: String
}

fun CreatedTypeContext.withFieldContext(
    fieldInfo: FieldInfo,
    fieldIRI: String,
    block: FieldContext.() -> Unit
) {
    object: CreatedTypeContext by this, FieldContext {
        override val fieldInfo: FieldInfo = fieldInfo
        override val fieldIRI: String = fieldIRI
    }.apply(block)
}
