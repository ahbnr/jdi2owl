package de.ahbnr.semanticweb.jdi2owl.debugging

import com.sun.jdi.ObjectReference
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.FieldInfo
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils.LocalVariableInfo

class ReferenceContexts {
    private val objectIdsToContexts = mutableMapOf<Long, MutableList<Context>>()

    fun addContext(ref: ObjectReference, context: Context) {
        val id = ref.uniqueID()
        val contextList = when (val existingList = objectIdsToContexts.getOrDefault(id, null)) {
            null -> {
                val newList = mutableListOf<Context>()
                objectIdsToContexts[id] = newList
                newList
            }
            else -> existingList
        }

        contextList.add(context)
    }

    fun getStackReferences(objectReference: ObjectReference): List<Context.ReferencedByStack> =
        objectIdsToContexts
            .getOrDefault(objectReference.uniqueID(), null)
            ?.filterIsInstance<Context.ReferencedByStack>()
            ?: emptyList()

    fun getReferencingFields(objectReference: ObjectReference): List<FieldInfo> =
        objectIdsToContexts
            .getOrDefault(objectReference.uniqueID(), null)
            ?.filterIsInstance<Context.ReferencedByField>()
            ?.map { it.fieldInfo }
            ?: listOf()

    sealed class Context {
        class ReferencedByStack(val variableInfo: LocalVariableInfo) : Context()
        class ReferencedByField(val fieldInfo: FieldInfo) : Context()
    }
}