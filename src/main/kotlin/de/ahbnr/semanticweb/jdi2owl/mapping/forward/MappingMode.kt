package de.ahbnr.semanticweb.jdi2owl.mapping.forward

abstract class MappingMode {
    internal abstract fun getMappers(): List<Mapper>
}