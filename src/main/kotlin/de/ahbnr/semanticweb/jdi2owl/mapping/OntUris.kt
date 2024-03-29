package de.ahbnr.semanticweb.jdi2owl.mapping

import com.sun.jdi.*
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.*
import org.apache.jena.atlas.lib.IRILib
import org.apache.jena.datatypes.xsd.XSDDatatype

@Suppress("PropertyName")
class OntIRIs(val ns: Namespaces) {
    inner class RdfIRIs {
        val type = ns.rdf + "type"

        val List = ns.rdf + "List"
        val first = ns.rdf + "first"
        val rest = ns.rdf + "rest"
        val nil = ns.rdf + "nil"
    }

    val rdf = RdfIRIs()

    inner class RdfsIRIs {
        val subClassOf = ns.rdfs + "subClassOf"
        val subPropertyOf = ns.rdfs + "subPropertyOf"
        val domain = ns.rdfs + "domain"
        val range = ns.rdfs + "range"
    }

    val rdfs = RdfsIRIs()

    inner class OwlIRIs {
        val Thing = ns.owl + "Thing"
        val Nothing = ns.owl + "Nothing"

        val Restriction = ns.owl + "Restriction"
        val onProperty = ns.owl + "onProperty"
        val onClass = ns.owl + "onClass"
        val someValuesFrom = ns.owl + "someValuesFrom"

        val Class = ns.owl + "Class"
        val ObjectProperty = ns.owl + "ObjectProperty"
        val DatatypeProperty = ns.owl + "DatatypeProperty"
        val FunctionalProperty = ns.owl + "FunctionalProperty"
        val InverseFunctionalProperty = ns.owl + "InverseFunctionalProperty"
        val cardinality = ns.owl + "cardinality"
        val NamedIndividual = ns.owl + "NamedIndividual"
        val equivalentClass = ns.owl + "equivalentClass"
        val unionOf = ns.owl + "unionOf"
        val oneOf = ns.owl + "oneOf"
        val inverseOf = ns.owl + "inverseOf"

        val annotatedTarget = ns.owl + "annotatedTarget"
        val annotatedSource = ns.owl + "annotatedSource"

        val maxQualifiedCardinality = ns.owl + "maxQualifiedCardinality"

        val sameAs = ns.owl + "sameAs"

        val AllDifferent = ns.owl + "AllDifferent"
        val members = ns.owl + "members"
    }

    val owl = OwlIRIs()

    inner class ShaclIRIs {
        val conforms = ns.sh + "conforms"
        val result = ns.sh + "result"
        val focusNode = ns.sh + "focusNode"
        val value = ns.sh + "value"
    }

    val sh = ShaclIRIs()

    inner class JavaIRIs {
        val UnpreparedType = ns.java + "UnpreparedType"
        val Class = ns.java + "Class"
        val Method = ns.java + "Method"
        val Field = ns.java + "Field"

        val hasName = ns.java + "hasName"

        val Interface = ns.java + "Interface"

        val ArrayType = ns.java + "ArrayType"
        val SequenceElement = ns.java + "SequenceElement"
        val `SequenceElement%3CObject%3E` = ns.java + IRILib.encodeUriComponent("SequenceElement<Object>")
        val UnpreparedTypeArray = ns.java + "UnpreparedTypeArray"
        val PrimitiveArray = ns.java + "PrimitiveArray"
        val PrimitiveSequenceElement = ns.java + "PrimitiveSequenceElement"
        val hasIndex = ns.java + "hasIndex"
        val hasElement = ns.java + "hasElement"
        val hasSuccessor = ns.java + "hasSuccessor"
        val storesPrimitive = ns.java + "storesPrimitive"
        val storesReference = ns.java + "storesReference"

        val VariableDeclaration = ns.java + "VariableDeclaration"

        val Location = ns.java + "Location"

        val hasMethod = ns.java + "hasMethod"
        val hasField = ns.java + "hasField"
        val declaresVariable = ns.java + "declaresVariable"
        val isDefinedAt = ns.java + "isDefinedAt"
        val isDeclaredAt = ns.java + "isDeclaredAt"
        val isAtSourcePath = ns.java + "isAtSourcePath"
        val isAtLine = ns.java + "isAtLine"

        val `null` = ns.java + "null"

        val Object = ns.java + "Object"
        val StackFrame = ns.java + "StackFrame"

        val `this` = ns.java + "this"

        val isAtStackDepth = ns.java + "isAtStackDepth"
        val hasUniqueId = ns.java + "hasUniqueId"

        val hasPlainValue = ns.java + "hasPlainValue"

        val isStatic = ns.java + "isStatic"

        val hasAccessModifier = ns.java + "hasAccessModifier"
        val AccessModifier = ns.java + "AccessModifier"

        fun genPrimitiveTypeIRI(type: TypeInfo.PrimitiveTypeInfo): String = when (type.jdiType) {
            is BooleanType -> XSDDatatype.XSDboolean
            is ByteType -> XSDDatatype.XSDbyte
            is CharType -> XSDDatatype.XSDunsignedShort
            is DoubleType -> XSDDatatype.XSDdouble
            is FloatType -> XSDDatatype.XSDfloat
            is IntegerType -> XSDDatatype.XSDint
            is LongType -> XSDDatatype.XSDlong
            is ShortType -> XSDDatatype.XSDshort
            else -> throw RuntimeException("Encountered unknown Java primitive type. This should never happen.")
        }.uri
    }

    val java = JavaIRIs()

    inner class ProgIRIs {
        val `java_lang_Object%5B%5D` = ns.prog + IRILib.encodeUriComponent("java.lang.Object[]")
        val java_lang_Object = ns.prog + IRILib.encodeUriComponent("java.lang.Object")

        fun genVariableDeclarationIRI(variableInfo: LocalVariableInfo): String =
            "${ns.prog}${IRILib.encodeUriComponent(variableInfo.rcn)}"

        fun genMethodIRI(methodInfo: MethodInfo): String =
            "${ns.prog}${IRILib.encodeUriComponent(methodInfo.rcn)}"

        fun genReferenceTypeIRI(referenceTypeInfo: TypeInfo.ReferenceTypeInfo): String {
            return "${ns.prog}${IRILib.encodeUriComponent(referenceTypeInfo.rcn)}"
        }

        fun genFieldIRI(fieldInfo: FieldInfo): String =
            "${ns.prog}${
                IRILib.encodeUriComponent( fieldInfo.rcn )
            }"

        fun genLocationIRI(locationInfo: LocationInfo): String =
            "${ns.prog}location_${IRILib.encodeUriComponent(locationInfo.id)}"

        fun genTypedHasElementIRI(componentTypeInfo: TypeInfo): String =
            "${ns.prog}hasElement${IRILib.encodeUriComponent("<${componentTypeInfo.rcn}>")}"

        fun genTypedSequenceElementIRI(componentTypeInfo: TypeInfo): String =
            "${ns.prog}SequenceElement${IRILib.encodeUriComponent("<${componentTypeInfo.rcn}>")}"

        fun genTypedStoresPrimitiveIRI(componentTypeInfo: TypeInfo.PrimitiveTypeInfo): String =
            "${ns.prog}storesPrimitive${IRILib.encodeUriComponent("<${componentTypeInfo.rcn}>")}"

        fun genTypedStoresReferenceIRI(componentTypeInfo: TypeInfo.ReferenceTypeInfo): String =
            "${ns.prog}storesReference${IRILib.encodeUriComponent("<${componentTypeInfo.rcn}>")}"
    }

    val prog = ProgIRIs()

    fun genTypeIRI(typeInfo: TypeInfo): String =
        when (typeInfo) {
            is TypeInfo.PrimitiveTypeInfo -> java.genPrimitiveTypeIRI(typeInfo)
            is TypeInfo.ReferenceTypeInfo -> prog.genReferenceTypeIRI(typeInfo)
            is TypeInfo.VoidTypeInfo -> throw RuntimeException("We do not map the void type. It has no IRI.")
        }

    inner class RunIRIs {
        val distinctObjectsAxiom = ns.run + "distinctObjectsAxiom"

        fun genFrameIRI(frameDepth: Int): String =
            "${ns.run}frame$frameDepth"

        private val objectIriPrefix = "${ns.run}object"

        fun genObjectIRI(objectReference: ObjectReference): String =
            "$objectIriPrefix${objectReference.uniqueID()}"

        fun isObjectIRI(uri: String) =
            uri.startsWith(objectIriPrefix)

        fun genSequenceElementInstanceIRI(containerRef: ObjectReference, index: Int) =
            "${ns.run}element${index}_of_${containerRef.uniqueID()}"
    }

    val run = RunIRIs()

    inner class LocalIRIs {
        val `this` = ns.local + "this"

        fun genLocalVariableIRI(variable: LocalVariableInfo): String =
            "${ns.local}${variable.localName}"
    }

    val local = LocalIRIs()

    inner class MacrosIRIs {
        val chainsProperties = ns.macros + "chainsProperties"
    }

    val macros = MacrosIRIs()

    /**
    /**
     * Type names may contain characters not allowed in IRI fragments or with special meaning, e.g. [] in `java.security.Permission[]`
     *
     * https://en.wikipedia.org/wiki/URI_fragment
     * https://datatracker.ietf.org/doc/html/rfc3986/#section-3.5
     *
     * This method will properly encode them.
    */
     *
    fun typeNameToIRIFragment(className: String): String {
    /**
     * The grammar for a fragment is:
     *       fragment    = *( pchar / "/" / "?" )
     * using this BNF syntax: https://datatracker.ietf.org/doc/html/rfc2234
     *
     * pchar is defined as
     *       pchar         = unreserved / pct-encoded / sub-delims / ":" / "@"
     *
     * These characters are unreserved: https://datatracker.ietf.org/doc/html/rfc3986/#section-2.3
     * And everything else must be encoded using percent encoding: https://datatracker.ietf.org/doc/html/rfc3986/#section-2.1
     *
     * The Java 11 type grammar is specified here:
     * https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html
     *
     * This can get complex, we rely on Apache Jena to safely encode:
    */
    return URIref.encode(className) // FIXME: Verify this is working
    }
    //FIXME: What about unicode?
     **/
}