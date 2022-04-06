package de.ahbnr.semanticweb.jdi2owl

import java.io.OutputStream

interface Logger {
    fun debug(line: String, appendNewline: Boolean = true)
    fun log(line: String, appendNewline: Boolean = true)
    fun emphasize(line: String, appendNewline: Boolean = true)
    fun success(line: String, appendNewline: Boolean = true)
    fun warning(line: String, appendNewline: Boolean = true)
    fun error(line: String, appendNewline: Boolean = true)

    fun logStream(): OutputStream
    fun warningStream(): OutputStream
    fun errorStream(): OutputStream
    fun successStream(): OutputStream
}

class BasicLogger: Logger {
    override fun debug(line: String, appendNewline: Boolean) =
        log(line, appendNewline)

    override fun log(line: String, appendNewline: Boolean) {
        if (appendNewline) println(line)
        else print(line)
    }

    override fun emphasize(line: String, appendNewline: Boolean) =
        log(line, appendNewline)

    override fun success(line: String, appendNewline: Boolean) =
        log(line, appendNewline)

    override fun warning(line: String, appendNewline: Boolean) =
        log(line, appendNewline)

    override fun error(line: String, appendNewline: Boolean) =
        log(line, appendNewline)

    override fun logStream(): OutputStream = System.out

    override fun warningStream(): OutputStream = logStream()

    override fun errorStream(): OutputStream = logStream()

    override fun successStream(): OutputStream = logStream()
}