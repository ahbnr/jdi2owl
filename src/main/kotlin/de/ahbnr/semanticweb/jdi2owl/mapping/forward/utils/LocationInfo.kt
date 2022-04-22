package de.ahbnr.semanticweb.jdi2owl.mapping.forward.utils

import com.sun.jdi.AbsentInformationException
import com.sun.jdi.Location

class LocationInfo(val id: String, val sourcePath: String, val line: Int) {
    companion object {
        fun fromJdiLocation(location: Location): LocationInfo? {
            val line = location.lineNumber()

            return try {
                if (line >= 0) {
                    LocationInfo(
                        "${location.hashCode()}",
                        location.sourcePath(),
                        line
                    )
                } else null
            } catch (e: AbsentInformationException) {
                null
            }
        }
    }
}