package de.ahbnr.semanticweb.jdi2owl.debugging.mirrors

import com.sun.jdi.ClassLoaderReference
import com.sun.jdi.ClassType
import com.sun.jdi.Method
import com.sun.jdi.ObjectReference
import com.sun.jdi.ThreadReference
import com.sun.jdi.VirtualMachine
import de.ahbnr.semanticweb.jdi2owl.debugging.mirrors.utils.MirroringError
import de.ahbnr.semanticweb.jdi2owl.debugging.mirrors.utils.retrieveInterface
import de.ahbnr.semanticweb.jdi2owl.debugging.mirrors.utils.retrieveMethod

class ClassLoaderMirror(
    private val classLoaderReference: ClassLoaderReference,
    private val thread: ThreadReference
) {
    class Static(
        private val classLoaderType: ClassType,
        private val thread: ThreadReference
    ) {
        companion object {
            fun getBuiltInClassLoaderType(thread: ThreadReference): Static? =
                thread
                    .virtualMachine()
                    .allClasses()
                    .filterIsInstance<ClassType>()
                    .find { it.classLoader() == null && it.name() == "java.lang.ClassLoader" }
                    ?.let { jdiType ->
                        Static(
                            jdiType,
                            thread
                        )
                    }
        }

        private val java_lang_ClassLoader_getSystemClassLoader: Method =
            retrieveMethod(classLoaderType, "getSystemClassLoader")

        fun getSystemClassLoader(): ClassLoaderReference {
            val systemClassLoader = classLoaderType.invokeMethod(
                thread, java_lang_ClassLoader_getSystemClassLoader, emptyList(), ObjectReference.INVOKE_SINGLE_THREADED
            )
                ?: throw MirroringError("getSystemClassLoader() method of java.lang.ClassLoader returned a null reference. This should never happen.")

            if (systemClassLoader !is ClassLoaderReference)
                throw MirroringError("getSystemClassLoader() method of java.lang.ClassLoader did not return a class loader. This should never happen.")

            return systemClassLoader
        }
    }
}