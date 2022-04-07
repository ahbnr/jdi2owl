package de.ahbnr.semanticweb.jdi2owl.tests.utils

import java.nio.file.Path

// Takes path relative to test resources folder, and returns absolute path to use for accessing the file
fun getTestSourceFile(path: String): Path =
    Path.of(
        Thread
            .currentThread()
            .contextClassLoader
            .getResource(path)!!
            .toURI()
    )