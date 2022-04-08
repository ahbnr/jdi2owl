package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.program_structure

import com.sun.jdi.AbsentInformationException
import com.sun.jdi.ClassNotLoadedException
import com.sun.jdi.ClassType
import com.sun.jdi.InterfaceType
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.TypeInfo
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.utils.addReferenceOrNullClass
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.JavaType
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.LocalVariableInfo
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.NodeFactory

fun mapVariableDeclarations(context: MethodContext) {
    context.apply {
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
}

fun mapVariableDeclaration(context: VariableDeclarationContext) {
    context.apply {
        // TODO: Include scope information?

        // it *is* a VariableDeclaration
        tripleCollector.addStatement(
            variableIRI,
            IRIs.rdf.type,
            IRIs.java.VariableDeclaration
        )

        // ...and it is declared by the surrounding method
        tripleCollector.addStatement(
            methodIRI,
            IRIs.java.declaresVariable,
            variableIRI
        )

        // ...and it is declared at this line:
        val line = variableInfo.getLine()
        if (line != null) {
            tripleCollector.addStatement(
                variableIRI,
                IRIs.java.isAtLine,
                NodeFactory.createLiteralByValue(line, XSDDatatype.XSDint)
            )
        }

        // Lets clarify the type of the variable and deal with unloaded types
        val variableType = try {
            JavaType.LoadedType(variableInfo.jdiLocalVariable.type())
        } catch (e: ClassNotLoadedException) {
            JavaType.UnloadedType(variableInfo.jdiLocalVariable.typeName())
        }

        // A variable declaration is modeled as a property that relates StackFrames and the variable values.
        // This allows to encode the typing of the variable into the property range.

        // The kind of property and range depend on the variable type:
        when (variableType) {
            is JavaType.LoadedType -> {
                when (val variableTypeInfo = buildParameters.typeInfoProvider.getTypeInfo(variableType.type)) {
                    is TypeInfo.ReferenceTypeInfo.CreatedType -> {
                        // If its a reference type, then it must be an ObjectProperty
                        tripleCollector.addStatement(
                            variableIRI,
                            IRIs.rdf.type,
                            IRIs.owl.ObjectProperty
                        )

                        // ...and the variable property ranges over the reference type of the variable
                        // and the null value:

                        val variableTypeIRI = IRIs.prog.genReferenceTypeIRI(variableTypeInfo)
                        tripleCollector.addStatement(
                            variableIRI,
                            IRIs.rdfs.range,
                            withCreatedTypeContext( variableTypeInfo, variableTypeIRI ) {
                                addReferenceOrNullClass(this)
                            }
                        )
                    }
                    is TypeInfo.PrimitiveTypeInfo -> {
                        tripleCollector.addStatement(
                            variableIRI,
                            IRIs.rdf.type,
                            IRIs.owl.DatatypeProperty
                        )

                        val datatypeURI = IRIs.java.genPrimitiveTypeURI(variableTypeInfo)
                        if (datatypeURI == null) {
                            logger.error("Unknown primitive data type: ${variableType.type}")
                            return
                        }

                        tripleCollector.addStatement(
                            variableIRI,
                            IRIs.rdfs.range,
                            datatypeURI
                        )
                    }
                    else -> logger.error("Encountered unknown kind of type: ${variableType.type}")
                }
            }
            is JavaType.UnloadedType -> {
                val variableTypeInfo = buildParameters.typeInfoProvider.getNotYetLoadedTypeInfo(variableType.typeName)
                val variableTypeIRI = IRIs.prog.genReferenceTypeIRI(variableTypeInfo)

                withNotYetLoadedTypeContext(variableTypeInfo, variableTypeIRI) {
                    mapNotYetLoadedType(this)
                }

                tripleCollector.addStatement(
                    variableIRI,
                    IRIs.rdf.type,
                    IRIs.owl.ObjectProperty
                )

                tripleCollector.addStatement(
                    variableIRI,
                    IRIs.rdfs.range,
                    variableTypeIRI
                )
            }
        }

        // Variables are always functional properties
        tripleCollector.addStatement(
            variableIRI,
            IRIs.rdf.type,
            IRIs.owl.FunctionalProperty
        )

        // The property domain is a StackFrame
        tripleCollector.addStatement(
            variableIRI,
            IRIs.rdfs.domain,
            IRIs.java.StackFrame
        )
    }
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
