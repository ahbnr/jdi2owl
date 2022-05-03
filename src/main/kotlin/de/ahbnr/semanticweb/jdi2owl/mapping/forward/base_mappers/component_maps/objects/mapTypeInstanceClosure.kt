package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.objects

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.program_structure.withRefTypeContext
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TypeInfo

fun mapTypeInstanceClosure(context: ObjectMappingContext) = with(context) {
    // Should we close all Java reference type?
    if (buildParameters.limiter.settings.closeReferenceTypes) {
        for (referenceType in buildParameters.jvmState.pausedThread.virtualMachine().allClasses()) {
            val typeInfo = buildParameters.typeInfoProvider.getTypeInfo(referenceType)
            val typeIRI = IRIs.prog.genReferenceTypeIRI(typeInfo)

            withRefTypeContext(
                typeInfo, typeIRI
            ) {
                val subtypes = (sequenceOf(typeInfo) + typeInfo.getAllSubtypes())
                    .filterIsInstance<TypeInfo.ReferenceTypeInfo.CreatedType>()
                    .map { it.jdiType }
                    .toSet()

                val instanceIRIs = allObjects
                    .filter { subtypes.contains(it.referenceType()) }
                    .map { IRIs.run.genObjectIRI(it) }
                    .toList()

                // If there are instances, we declare the type equivalent to a nominal containing all its instances
                if (instanceIRIs.isNotEmpty()) {
                    with (IRIs) {
                        tripleCollector.dsl {
                            typeIRI {
                                owl.oneOf of instanceIRIs
                            }
                        }
                    }
                }

                else {
                    // Otherwise, we declare the type to be subsumed by owl:Nothing.
                    // This is because empty OneOf nominals are not supported
                    tripleCollector.addStatement(
                        typeIRI,
                        IRIs.rdfs.subClassOf,
                        IRIs.owl.Nothing
                    )
                }
            }
        }
    }
}

