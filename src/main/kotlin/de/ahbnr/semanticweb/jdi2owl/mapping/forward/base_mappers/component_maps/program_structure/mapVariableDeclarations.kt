package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.program_structure

import com.sun.jdi.AbsentInformationException
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.mapJavaNameToLiteral
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.LocalVariableInfo
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TypeInfo
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.NodeFactory

fun mapVariableDeclarations(context: MethodContext) = with(context) {
    val variables = try {
            methodInfo.jdiMethod.variables()
        }
        catch(e: AbsentInformationException) {
            return
        }

    for (variable in variables) {
        val variableInfo = methodInfo.getVariableInfo(variable)
        val variableIRI = IRIs.prog.genVariableDeclarationIRI(variableInfo)

        withVariableDeclarationContext(variableInfo, variableIRI) {
            mapVariableDeclaration(this)
        }
    }
}

fun mapVariableDeclaration(context: VariableDeclarationContext) = with(context) {
    // TODO: Include scope information?

    // A variable declaration is modeled as a property that relates StackFrames and the variable values.
    // This allows to encode the typing of the variable into the property range.

    with(IRIs) {
        tripleCollector.dsl {
            variableIRI {
                // declare variable as individual that is a variable
                rdf.type of owl.NamedIndividual
                rdf.type of java.VariableDeclaration
                rdf.type of owl.FunctionalProperty
                rdfs.domain of java.StackFrame

                java.hasName of mapJavaNameToLiteral(variableInfo.localName)
            }

            methodIRI {
                // the variable is declared by the surrounding method
                java.declaresVariable of variableIRI
            }
        }
    }

    val variableTypeInfo = variableInfo.typeInfo
    val variableTypeIRI = IRIs.genTypeIRI(variableTypeInfo)

    when (variableTypeInfo) {
        is TypeInfo.PrimitiveTypeInfo -> {
            with(IRIs) {
                tripleCollector.dsl {
                    variableIRI {
                        rdf.type of owl.DatatypeProperty
                        rdfs.range of variableTypeIRI
                    }
                }
            }
        }
        is TypeInfo.ReferenceTypeInfo -> {
            with(IRIs) {
                tripleCollector.dsl {
                    variableIRI {
                        rdf.type of owl.ObjectProperty
                        rdfs.range of (variableTypeIRI `⊔` oneOf(java.`null`))
                    }
                }
            }

            if (variableTypeInfo is TypeInfo.ReferenceTypeInfo.NotYetLoadedType) {
                withNotYetLoadedTypeContext(variableTypeInfo, variableTypeIRI) {
                    mapNotYetLoadedType(this)
                }
            }
        }
    }

    // Include line info, if we have it
    val line = variableInfo.getLine()
    if (line != null) {
        tripleCollector.addStatement(
            variableIRI,
            IRIs.java.isAtLine,
            NodeFactory.createLiteralByValue(line, XSDDatatype.XSDint)
        )
    }

    pluginListeners.mapInContext(context)
}

interface VariableDeclarationContext: MethodContext {
    val variableInfo: LocalVariableInfo
    val variableIRI: String
}

fun MethodContext.withVariableDeclarationContext(
    variableInfo: LocalVariableInfo,
    variableIRI: String,
    block: VariableDeclarationContext.() -> Unit
) {
    object: MethodContext by this, VariableDeclarationContext {
        override val variableInfo: LocalVariableInfo = variableInfo
        override val variableIRI: String = variableIRI
    }.apply(block)
}
