package de.ahbnr.semanticweb.jdi2owl.mapping.forward.mappers.component_maps.runtime_components

import com.sun.jdi.ObjectReference
import com.sun.jdi.ThreadReference
import de.ahbnr.semanticweb.jdi2owl.debugging.mirrors.*
import de.ahbnr.semanticweb.jdi2owl.debugging.mirrors.PrimitiveWrapperMirror
import de.ahbnr.semanticweb.jdi2owl.debugging.mirrors.utils.MirroringError
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.TypeInfo
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.extendsClass

fun mapPrimitiveWrapperObject(context: ObjectContext) = with(context) {
    // Only class instances can be instances of wrappers
    val typeInfo = context.typeInfo
        as? TypeInfo.ReferenceTypeInfo.CreatedType.ClassOrInterface.Class
        ?: return

    // If this the object is of a wrapper class for a primitive value, we extract the primitive value and directly
    // represent it in the knowledge base
    val wrapperObjectCasesX: Map<String, (ObjectReference, ThreadReference) -> PrimitiveWrapperMirror<*, *>> =
        mapOf(
            "java.lang.Byte" to ::ByteWrapperMirror,
            "java.lang.Short" to ::ShortWrapperMirror,
            "java.lang.Integer" to ::IntegerWrapperMirror,
            "java.lang.Long" to ::LongWrapperMirror,
            "java.lang.Float" to ::FloatWrapperMirror,
            "java.lang.Double" to ::DoubleWrapperMirror,
            "java.lang.Character" to ::CharacterWrapperMirror,
        )

    for ((wrapperClassName, mirrorConstructor) in wrapperObjectCasesX) {
        if (typeInfo.jdiType.extendsClass(wrapperClassName)) {
            try {
                val mirror = mirrorConstructor(`object`, buildParameters.jvmState.pausedThread)
                val valueMirror = mirror.valueMirror()
                val valueNode = valueMapper.map(valueMirror)

                if (valueNode != null) {
                    tripleCollector.addStatement(
                        objectIRI,
                        IRIs.java.hasPlainValue,
                        valueNode
                    )
                }
            } catch (e: MirroringError) {
                logger.error(e.message)
            }
        }
    }
}