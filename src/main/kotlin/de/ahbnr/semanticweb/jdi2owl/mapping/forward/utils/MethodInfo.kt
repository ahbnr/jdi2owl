package de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils

import com.sun.jdi.AbsentInformationException
import com.sun.jdi.ClassNotLoadedException
import com.sun.jdi.LocalVariable
import com.sun.jdi.Method
import de.ahbnr.semanticweb.jdi2owl.debugging.utils.InternalJDIUtils
import de.ahbnr.semanticweb.jdi2owl.Logger
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.HasRCN
import de.ahbnr.semanticweb.jdi2owl.mapping.forward.TypeInfoProvider
import org.apache.commons.collections4.MultiValuedMap
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.time.measureDuration
import spoon.reflect.CtModel
import spoon.reflect.code.CtBlock
import spoon.reflect.code.CtLocalVariable
import spoon.reflect.cu.position.NoSourcePosition
import spoon.reflect.declaration.CtElement
import spoon.reflect.declaration.CtMethod
import spoon.reflect.path.CtPathStringBuilder
import spoon.reflect.visitor.filter.LocalVariableScopeFunction
import spoon.reflect.visitor.filter.TypeFilter

class MethodInfo(
    val typeInfoProvider: TypeInfoProvider,
    val jdiMethod: Method
): HasRCN, KoinComponent {
    private val logger: Logger by inject()

    override val rcn: String
    init {
        val declaringTypeInfo = typeInfoProvider.getTypeInfo(jdiMethod.declaringType())

        // we have to encode the return type to deal with bridge methods which can violate the overloading rules
        // TODO: Mention this in report
        val returnTypeInfo = try {
            typeInfoProvider.getTypeInfo(jdiMethod.returnType())
        } catch (e: ClassNotLoadedException) {
            typeInfoProvider.getNotYetLoadedTypeInfo(jdiMethod.returnTypeName())
        }

        // The JDI either lets us access all argument types if all have been created, or none at all, if at least one
        // of them has not been created.
        // In that case, only the binary names of all argument types are available.
        //
        // However, since we need the correct RCN for each one of them, we can not just work with the binary names.
        // Hence, we access internal APIs to access argument types individually, if they have been created.
        val numArgs = InternalJDIUtils.Method_argumentSignatures(jdiMethod).size
        val argTypeNames = jdiMethod.argumentTypeNames()
        val argumentRCNs = (0 until numArgs).map { i ->
            try {
                typeInfoProvider
                    .getTypeInfo(InternalJDIUtils.Method_argumentType(jdiMethod, i))
                    .rcn
            }

            catch (e: ClassNotLoadedException) {
                typeInfoProvider
                    .getNotYetLoadedTypeInfo(argTypeNames[i])
                    .rcn
            }
        }

        rcn = "${declaringTypeInfo.rcn}.-${returnTypeInfo.rcn}-${jdiMethod.name()}(${argumentRCNs.joinToString(",")})"
    }

    fun getDeclarationLocation(sourceModel: CtModel): LocationInfo? {
        val sourcePosition = getMethodSource(sourceModel)?.position
        return if (sourcePosition != null) {
            LocationInfo.fromSourcePosition(sourcePosition)
        } else null
    }

    val definitionLocation: LocationInfo?
        get() {
            val jdiLocation = jdiMethod.location()
            return if (jdiLocation != null) {
                LocationInfo.fromJdiLocation(jdiLocation)
            } else null
        }

    fun getVariableInfo(jdiVariable: LocalVariable) =
        LocalVariableInfo(jdiVariable, this)

    private val jdiVariables: List<LocalVariable> by lazy {
        (if (!jdiMethod.isAbstract && !jdiMethod.isNative)
            try {
                jdiMethod.variables()
            } catch (e: AbsentInformationException) {
                if (AbsentInformationPackages.none { jdiMethod.declaringType().name().startsWith(it) }) {
                    logger.debug("Unable to get variables for $jdiMethod. This can happen for native and abstract methods.")
                }
                null
            }
        else null)
            ?: listOf()
    }

    private fun getMethodSource(sourceModel: CtModel): CtMethod<*>? {
        val referenceType = jdiMethod.declaringType()

        val path = CtPathStringBuilder().fromString(
            ".${referenceType.name()}#method[signature=${jdiMethod.name()}(${
                jdiMethod.argumentTypeNames().joinToString(",")
            })]"
        )

        return path
            .evaluateOn<CtMethod<*>>(sourceModel.rootPackage)
            .firstOrNull()
    }

    private fun getBody(sourceModel: CtModel): CtBlock<*>? =
        getMethodSource(sourceModel)?.body

    private fun getSourceVariables(sourceModel: CtModel): List<CtLocalVariable<*>> =
        getBody(sourceModel)?.getElements(TypeFilter(CtLocalVariable::class.java)) ?: listOf()

    private fun getJdiVarToSource(sourceModel: CtModel): MultiValuedMap<LocalVariable, CtLocalVariable<*>> {
        val jdiToSource = ArrayListValuedHashMap<LocalVariable, CtLocalVariable<*>>()

        val sourceVariables = getSourceVariables(sourceModel)
        val sourceVarToScope = sourceVariables.associateWith { sourceVariable ->
            val scopeElements = sourceVariable
                .map(LocalVariableScopeFunction())
                .list<CtElement>()
                .filter { it.position !is NoSourcePosition }
            val sourceMinScopeLine = sourceVariable.position.line
            val sourceMaxScopeLine = scopeElements.maxOf { it.position.endLine }

            sourceMinScopeLine..sourceMaxScopeLine
        }

        for (jdiVariable in jdiVariables) {
            for (sourceVariable in sourceVariables) {
                val jdiMinScopeLine = InternalJDIUtils.getScopeStart(jdiVariable).lineNumber()
                val sourceScope = sourceVarToScope[sourceVariable]!!

                if (jdiMinScopeLine >= 0 && jdiVariable.name() == sourceVariable.simpleName && sourceScope.contains(
                        jdiMinScopeLine
                    )
                ) {
                    jdiToSource.put(jdiVariable, sourceVariable)
                    break
                }
            }
        }

        return jdiToSource
    }

    private fun findSources(
        sourceModel: CtModel,
        sourceVariables: List<CtLocalVariable<*>>,
        simpleName: String,
        groupedJdiVars: List<LocalVariable>
    ): Map<LocalVariable, CtLocalVariable<*>?> {
        val sameNameSources = sourceVariables.filter { it.simpleName == simpleName }

        // If all variable instances present in the source are also present at runtime, we just have to match them
        // in order
        // FIXME: This depends on the implementation of Comparable for LocalVariables of the used JDI implementation
        //   it should be correct for the JDI shipped with Java 11..
        if (sameNameSources.size == groupedJdiVars.size) {
            return groupedJdiVars
                .sorted()
                .zip(sameNameSources.sortedBy { it.position.sourceStart })
                .toMap()
        }

        // It seems some variables with the same name have been omitted by the compiler.
        // Lets try to match them by their location.
        else {
            val jdiVarToSource = getJdiVarToSource(sourceModel)
            return groupedJdiVars.associateWith {
                val sourceCandidates = jdiVarToSource[it]!!

                if (sourceCandidates.size == 1)
                    sourceCandidates.first()
                else null
            }
        }
    }
}