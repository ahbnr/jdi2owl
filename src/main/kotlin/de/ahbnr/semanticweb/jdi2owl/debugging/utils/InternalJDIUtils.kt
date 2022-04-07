@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package de.ahbnr.semanticweb.jdi2owl.debugging.utils

import com.sun.jdi.*
import com.sun.tools.jdi.LocalVariableImpl
import com.sun.tools.jdi.ObjectReferenceImpl
import com.sun.tools.jdi.MethodImpl
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException

object InternalJDIUtils {
    private val scopeStartField: Field = LocalVariableImpl::class.java.getDeclaredField("scopeStart")
    private val scopeEndField: Field = LocalVariableImpl::class.java.getDeclaredField("scopeEnd")
    private val slotField: Field = LocalVariableImpl::class.java.getDeclaredField("slot")
    private val objectReferenceImplConstructor: Constructor<ObjectReferenceImpl>
    private val MethodImpl_argumentType: java.lang.reflect.Method
    private val MethodImpl_argumentSignatures: java.lang.reflect.Method

    init {
        scopeStartField.isAccessible = true
        scopeEndField.isAccessible = true
        slotField.isAccessible = true

        objectReferenceImplConstructor = ObjectReferenceImpl::class.java.declaredConstructors.find {
            it.parameterCount == 2 &&
                    it.parameterTypes[0].isAssignableFrom(VirtualMachine::class.java) &&
                    it.parameterTypes[1].canonicalName == "long"
        } as Constructor<ObjectReferenceImpl>?
            ?: throw RuntimeException("Could not retrieve internal JDI ObjectReferenceImpl constructor. Are you compiling with the OpenJDK 11?")
        objectReferenceImplConstructor.isAccessible = true

        MethodImpl_argumentType = MethodImpl::class.java.declaredMethods.find {
            it.name == "argumentType" &&
            it.parameterCount == 1 &&
            it.parameterTypes[0].canonicalName == "int"
        } ?: throw RuntimeException("Could not retrieve internal JDI method MethodImpl.argumentType(int) method. Are you compiling with the OpenJDK 11?")
        MethodImpl_argumentType.isAccessible = true

        MethodImpl_argumentSignatures = MethodImpl::class.java.getDeclaredMethod("argumentSignatures")
            ?: throw RuntimeException("Could not retrieve internal JDI method MethodImpl.argumentSignatures() method. Are you compiling with the OpenJDK 11?")
        MethodImpl_argumentSignatures.isAccessible = true
    }

    fun getSlot(variable: LocalVariable) =
        slotField.get(variable) as Int

    fun getScopeStart(variable: LocalVariable) =
        scopeStartField.get(variable) as Location

    fun getScopeEnd(variable: LocalVariable) =
        scopeEndField.get(variable) as Location

    fun objectReferenceFromId(vm: VirtualMachine, objectId: Long) =
        objectReferenceImplConstructor.newInstance(vm, objectId)

    @Throws(ClassNotLoadedException::class)
    fun Method_argumentType(jdiMethod: Method, index: Int): Type =
        try {
            MethodImpl_argumentType.invoke(jdiMethod, index)
                    as? Type
                ?: throw RuntimeException("Internal JDI method MethodImpl.argumentType(int) did not return Type instance. Are you compiling with the OpenJDK 11?")
        }

        catch (e: InvocationTargetException) {
            when (val cause = e.cause) {
                null -> throw e
                else -> throw cause
            }
        }

    fun Method_argumentSignatures(jdiMethod: Method): List<String> =
        MethodImpl_argumentSignatures.invoke(jdiMethod)
            as? List<String>
            ?: throw RuntimeException("Internal JDI method MethodImpl.argumentSignatures() did not return list. Are you compiling with the OpenJDK 11?")
}

