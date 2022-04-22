package de.ahbnr.semanticweb.jdi2owl.mapping.forward

object BaseMapping: MappingMode() {
    override fun getMappers(): List<Mapper> = getBaseMappers()
}