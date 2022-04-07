package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure

import de.ahbnr.semanticweb.jdi2owl.mapping.datatypes.JavaAccessModifierDatatype
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.MethodInfo

fun mapMethods(context: CreatedTypeContext) {
    context.apply {
        for (method in typeInfo.jdiType.methods()) { // inherited methods are not included!
            val methodInfo = typeInfo.getMethodInfo(method)
            val methodIRI = IRIs.prog.genMethodIRI(methodInfo)

            withMethodContext(methodInfo, methodIRI) {
                mapMethod(this)
            }
        }
    }
}

fun mapMethod(context: MethodContext) {
    context.apply {
        if (buildParameters.limiter.canMethodBeSkipped(methodInfo.jdiMethod))
            return


        // The methodSubject *is* a method
        tripleCollector.addStatement(
            methodIRI,
            IRIs.rdf.type,
            IRIs.java.Method
        )

        // ...and the class contains the method
        tripleCollector.addStatement(
            typeIRI,
            IRIs.java.hasMethod,
            methodIRI
        )

        // access modifiers
        tripleCollector.addStatement(
            methodIRI,
            IRIs.java.hasAccessModifier,
            JavaAccessModifierDatatype
                .AccessModifierLiteral
                .fromJdiAccessible(methodInfo.jdiMethod)
                .toNode()
        )

        if (buildParameters.limiter.canMethodDetailsBeSkipped(methodInfo.jdiMethod)) {
            return
        }

        // ...and the method declares some variables
        mapVariableDeclarations(this)

        // Where in the source code is the method?
        mapMethodLocation(this)
    }
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
