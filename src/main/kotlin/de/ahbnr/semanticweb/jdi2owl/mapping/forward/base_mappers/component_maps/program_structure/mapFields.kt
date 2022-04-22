package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.program_structure

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.FieldInfo
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TypeInfo
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.NodeFactory

fun mapFields(context: CreatedTypeContext) = with(context) {
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

fun mapField(context: FieldContext) = with(context) {
    if (buildParameters.limiter.canFieldBeSkipped(fieldInfo.jdiField))
        return

    // Fields are modeled as properties.
    // This way, the field type can be encoded in the property range.
    // The exact kind of property and ranges now depend on the field type

    with(IRIs) {
        tripleCollector.dsl {
            fieldIRI {
                // this field is an individual that is a java field
                rdf.type of owl.NamedIndividual
                rdf.type of java.Field

                // Is it a static field?
                java.isStatic of NodeFactory.createLiteralByValue(fieldInfo.jdiField.isStatic, XSDDatatype.XSDboolean)

                // Fields are functional properties.
                // For every instance of the field (there is only one for the object) there is exactly one property value
                rdf.type of owl.FunctionalProperty

                // Domain
                if (fieldInfo.jdiField.isStatic)
                    // static field instances are defined for the class itself, not the instances
                    rdfs.domain of oneOf(typeIRI) // Punning: We treat the class as an individual
                else
                    // an instance field is a thing defined for every object instance of the class concept via rdfs:domain
                    // (this also drives some implications, e.g. if there exists a field, there must also be some class it belongs to etc)
                    rdfs.domain of typeIRI
            }

            typeIRI {
                // the field is part of the surrounding class
                java.hasField of fieldIRI
            }
        }
    }
    val fieldTypeInfo = fieldInfo.typeInfo
    val fieldTypeIRI = IRIs.genTypeIRI(fieldTypeInfo)
    when (fieldTypeInfo) {
        is TypeInfo.PrimitiveTypeInfo -> {
            with(IRIs) {
                tripleCollector.dsl {
                    fieldIRI {
                        rdf.type of owl.DatatypeProperty
                        rdfs.range of fieldTypeIRI
                    }
                }
            }
        }

        is TypeInfo.ReferenceTypeInfo -> {
            val fieldTypeIRI = IRIs.prog.genReferenceTypeIRI(fieldTypeInfo)

            with(IRIs) {
                tripleCollector.dsl {
                    fieldIRI {
                        // Since the Java field is of a reference type here, it must be an ObjectProperty,
                        // that is, a property that links individuals to individuals
                        // (here: Java class instances (parent object) to Java class instances (field value))
                        rdf.type of owl.ObjectProperty

                        rdfs.range of (fieldTypeIRI `⊔` oneOf(java.`null`))
                    }
                }
            }

            if (fieldTypeInfo is TypeInfo.ReferenceTypeInfo.NotYetLoadedType) {
                withNotYetLoadedTypeContext(
                    fieldTypeInfo,
                    fieldTypeIRI
                ) {
                    mapNotYetLoadedType(this)
                }
            }
        }
    }

    if (!fieldInfo.jdiField.isStatic) {
        // We force all individuals of the class to implement the field
        // FIXME: This creates a lot of subClass axioms. Inefficient.
        //   It is a form of closure axiom. Should we really enforce this?
        //   It does create reasoning overhead
        // tripleCollector.addStatement(
        //     classIRI,
        //     IRIs.rdfs.subClassOf,
        //     tripleCollector.addConstruct(
        //         TripleCollector.BlankNodeConstruct.OWLSome(
        //             fieldIRI,
        //             addReferenceOrNullClass(IRIs.prog.genReferenceTypeIRI(fieldType.type))
        //         )
        //     )
        // )

        // We force all individuals of the class to implement these fields
        // DATATYPE VARIANT
        // FIXME: This creates a lot of subClass axioms. Inefficient.
        //   It is a form of closure axiom. Should we really enforce this?
        //   It does create reasoning overhead
        // tripleCollector.addStatement(
        //     classIRI,
        //     IRIs.rdfs.subClassOf,
        //     tripleCollector.addConstruct(
        //         TripleCollector.BlankNodeConstruct.OWLSome(
        //             fieldIRI,
        //             NodeFactory.createIRI(datatypeIRI)
        //         )
        //     )
        // )
    }

    pluginListeners.mapInContext(context)
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
