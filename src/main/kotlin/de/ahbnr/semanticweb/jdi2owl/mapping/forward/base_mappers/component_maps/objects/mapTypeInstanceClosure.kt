package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.objects

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.program_structure.withRefTypeContext

fun mapTypeInstanceClosure(context: ObjectMappingContext) = with(context) {
    // Should we close all Java reference type?
    if (buildParameters.limiter.settings.closeReferenceTypes) {
        for (referenceType in buildParameters.jvmState.pausedThread.virtualMachine().allClasses()) {
            val typeInfo = buildParameters.typeInfoProvider.getTypeInfo(referenceType)
            val typeIRI = IRIs.prog.genReferenceTypeIRI(typeInfo)

            withRefTypeContext(
                typeInfo, typeIRI
            ) {
                // val instances = referenceType.instances(Long.MAX_VALUE)
                val instanceIRIs = allObjects
                    .filter { it.referenceType() == referenceType }
                    .map { IRIs.run.genObjectIRI(it) }

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

