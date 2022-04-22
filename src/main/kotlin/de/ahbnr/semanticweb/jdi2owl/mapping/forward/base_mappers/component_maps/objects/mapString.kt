package de.ahbnr.semanticweb.jdi2owl.mapping.forward.base_mappers.component_maps.objects

import com.sun.jdi.StringReference
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TripleCollector
import dk.brics.automaton.Datatypes
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.NodeFactory

fun mapString(context: ObjectContext): Unit = with(context) {
    val `object` = `object`
        as? StringReference
        ?: return

    /**
     * FIXME: Talk about this with SIRIUS
     *
     * Usability feature: During debugging, we want to give developers easy, human-readable access to
     * java.lang.String values.
     * Therefore, we define the java:hasPlainValue data property for Java strings.
     * This leaves only the question, which datatype to use:
     *
     * The OWL 2 datatype map (https://www.w3.org/TR/owl2-syntax/#Datatype_Maps) offers some types to store
     * strings, e.g. xsd:string.
     *
     * However, none of these types can store arbitrary bytes like java.lang.String can, e.g.
     * new String(new byte[] { 0 }).
     * For example, xsd:string may only contain characters matching the XML Char production:
     *  	Char	   ::=   	[#x1-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
     *
     * See also
     * * https://www.w3.org/TR/xmlschema11-2/#string
     * * https://www.w3.org/TR/xml11/#NT-Char
     *
     * The HermiT reasoner will check this and throw exceptions for strings that contain zero bytes etc.
     * (We noticed this, when running the performance case study on DaCapo lusearch)
     *
     * It seems rdf:PlainLiteral would support the entire range of values, since it supports all characters
     * from the unicode value set:
     *
     * * https://www.w3.org/TR/2012/REC-rdf-plain-literal-20121211/
     * * https://www.w3.org/TR/rdf-concepts/#dfn-plain-literal
     * * https://www.w3.org/TR/rdf-concepts/#dfn-lexical-form
     *
     * However, apparently rdf:PlainLiteral is not part of the OWL 2 datatype map and HermiT will
     * automatically type plain literals with xsd:string.
     *
     * This leaves only the binary representation types xsd:hexBinary and xsd:base64Binary.
     * However, using these would defeat the purpose of making string values easily accessible and
     * human-readable.
     *
     * Therefore, we decided, to only set the java:hasPlainValue property for strings which match the
     * value restrictions of xsd:string.
     * For every other string, we make it clear that there is no plain representation by restricting the
     * cardinality of java:hasPlainValue to 0.
     *
     * Btw. there is some additional literature on storing binary data in rdf: There is some literature on storing arbitrary binary data in RDF: https://books.google.de/books?id=AKiPuLrMFl8C&pg=PA47&lpg=PA47&dq=RDF+literals+binary+data&source=bl&ots=KJCgFRDKiF&sig=ACfU3U22tEGXlJCBtDMNmZz0o_LTUgp69w&hl=en&sa=X&ved=2ahUKEwiz67if0a_2AhWDtqQKHanDB94Q6AF6BAgnEAM#v=onepage&q=RDF%20literals%20binary%20data&f=false
     */
    val stringValue = `object`.value()
    // The check provided by Jena does not seem to do much of anything:
    // if (XSDDatatype.XSDstring.isValidValue(stringValue)) {
    // So we use the same check as HermiT:
    val checker = Datatypes.get("string")
    if (checker.run(stringValue)) {
        tripleCollector.addStatement(
            objectIRI,
            IRIs.java.hasPlainValue,
            NodeFactory.createLiteralByValue(
                stringValue, // FIXME: Usually some symbols, e.g. <, &, " must be escaped in some situations. Jena seems to do it in some cases, but we should check this
                XSDDatatype.XSDstring
            )
        )
    } else {
        tripleCollector.addStatement(
            objectIRI,
            IRIs.rdf.type,
            tripleCollector.addConstruct(
                TripleCollector.BlankNodeConstruct.OWLDataCardinalityRestriction(
                    onPropertyUri = IRIs.java.hasPlainValue,
                    cardinality = TripleCollector.BlankNodeConstruct.CardinalityType.Exactly(0)
                )
            )
        )
    }
}

