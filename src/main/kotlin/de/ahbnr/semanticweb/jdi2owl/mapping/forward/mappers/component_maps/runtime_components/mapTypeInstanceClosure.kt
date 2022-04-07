package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.runtime_components

import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure.withRefTypeContext
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TripleCollector

fun mapTypeInstanceClosure(context: ObjectMappingContext) = with(context) {
    // Should we close all Java reference type?
    if (buildParameters.limiter.settings.closeReferenceTypes) {
        for (referenceType in buildParameters.jvmState.pausedThread.virtualMachine().allClasses()) {
            val typeInfo = buildParameters.typeInfoProvider.getTypeInfo(referenceType)
            val typeIRI = IRIs.prog.genReferenceTypeURI(typeInfo)

            withRefTypeContext(
                typeInfo, typeIRI
            ) {
                // val instances = referenceType.instances(Long.MAX_VALUE)
                val instanceIRIs = allObjects
                    .filter { it.referenceType() == referenceType }
                    .map { IRIs.run.genObjectURI(it) }

                // If so, declare each type equivalent to a nominal containing all its instances
                tripleCollector.addStatement(
                    typeIRI,
                    IRIs.owl.equivalentClass,
                    tripleCollector.addConstruct(
                        TripleCollector.BlankNodeConstruct.OWLOneOf.fromIRIs(instanceIRIs)
                    )
                )
            }
        }
    }
}

