package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.runtime_components

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure.withRefTypeContext
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TripleCollector

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
                    .map { IRIs.run.genObjectURI(it) }

                // If there are instances, we declare the type equivalent to a nominal containing all its instances
                if (instanceIRIs.isNotEmpty()) {
                    tripleCollector.addStatement(
                        typeIRI,
                        IRIs.owl.equivalentClass,
                        tripleCollector.addConstruct(
                            TripleCollector.BlankNodeConstruct.OWLOneOf.fromIRIs(instanceIRIs)
                        )
                    )
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

