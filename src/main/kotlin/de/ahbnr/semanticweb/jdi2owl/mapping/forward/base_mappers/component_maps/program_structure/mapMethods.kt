package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.program_structure

import de.ahbnr.semanticweb.jdi2owl.mapping.datatypes.JavaAccessModifierDatatype
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.MethodInfo

fun mapMethods(context: CreatedTypeContext) = with(context) {
    for (method in typeInfo.jdiType.methods()) { // inherited methods are not included!
        val methodInfo = typeInfo.getMethodInfo(method)
        val methodIRI = IRIs.prog.genMethodIRI(methodInfo)

        withMethodContext(methodInfo, methodIRI) {
            mapMethod(this)
        }
    }
}

fun mapMethod(context: MethodContext): Unit = with(context) {
    if (buildParameters.limiter.canMethodBeSkipped(methodInfo.jdiMethod))
        return

    with(IRIs) {
        tripleCollector.dsl {
            methodIRI {
                // declare the IRI as an individual that is a method
                rdf.type of owl.NamedIndividual
                rdf.type of java.Method

                // access modifiers
                java.hasAccessModifier of JavaAccessModifierDatatype
                    .AccessModifierLiteral
                    .fromJdiAccessible(methodInfo.jdiMethod)
                    .toNode()
            }

            typeIRI {
                // The class declaring *has* the method
                java.hasMethod of methodIRI
            }
        }
    }

    if (buildParameters.limiter.canMethodDetailsBeSkipped(methodInfo.jdiMethod))
        return

    // ...and the method declares some variables
    mapVariableDeclarations(this)

    // Where in the source code is the method?
    mapMethodLocation(this)

    // allow plugins to extend method mapping
    pluginListeners.mapInContext(this)
}

interface MethodContext: CreatedTypeContext {
    val methodInfo: MethodInfo
    val methodIRI: String
}

fun CreatedTypeContext.withMethodContext(
    methodInfo: MethodInfo,
    methodIRI: String,
    block: MethodContext.() -> Unit
) {
    object: CreatedTypeContext by this, MethodContext {
        override val methodInfo: MethodInfo = methodInfo
        override val methodIRI: String = methodIRI
    }.apply(block)
}
