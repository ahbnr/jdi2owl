# jdi2owl: Mapping Java program states to OWL knowledge bases :owl:
![Build and Test](https://github.com/ahbnr/jdi2owl/actions/workflows/build.yml/badge.svg)

This library is part of of the [Semantic Java Debugger](https://github.com/ahbnr/SemanticJavaDebugger) (`sjdb`) project.
Please see the Semantic Java Debugger repository or [my thesis](https://tuprints.ulb.tu-darmstadt.de/22143/) for context.

One of the core components of `sjdb` is a mapping of Java program states to [OWL](https://www.w3.org/TR/2012/REC-owl2-overview-20121211/) knowledge bases, see chapters 4, 5, and 6 of my thesis.
As suggested by Kamburjan et al. [(link)](https://doi.org/10.1007/978-3-030-77385-4_8), semantic debugging is only one possible application of such a mapping.
They also argue that such mappings of program states could be used for the verification of class invariants, or to implement internal semantic state access which allows programs to semantically query their own state.

Due to such potential future applications of `sjdb`'s mapping of Java program states, the implementation of the mapping has been separated from the interactive debugger `sjdb` into this independent library.
It is called `jdi2owl` and it can be integrated into any JVM-based Kotlin application or Java application.

The `jdi2owl` library contains functionalities for managing debuggees through the JDI and for extracting their state, see section 8.2 of my thesis.
It also contains the knowledge base construction algorithms and the associated plugin system presented in section 8.3.

## Prerequisites :clipboard:

This guide assumes, that all commands in the following sections are
executed in the `bash` shell of a linux system.

Please make sure that the following dependencies are available on your system:

* OpenJDK 11 (other Java implementations, like the Oracle JDK may not be compatible!)

## Building :hammer_and_wrench:

You can build the library with gradle:

```sh
./gradlew build
```

## Usage :thinking:

There is no tutorial yet, but you can look at some usage examples.

For some very basic examples, you can look at the tests in [`src/test/kotlin/de/ahbnr/semanticweb/jdi2owl/tests`](https://github.com/ahbnr/jdi2owl/tree/main/src/test/kotlin/de/ahbnr/semanticweb/jdi2owl/tests).
E.g. [`HelloWorldTest.kt`](https://github.com/ahbnr/jdi2owl/blob/main/src/test/kotlin/de/ahbnr/semanticweb/jdi2owl/tests/HelloWorldTest.kt) shows how to map the state of a basic "Hello World"-program, and demonstrates some simple inspections performed on the resulting knowledge base.

The main usage example is the [Semantic Java Debugger](https://github.com/ahbnr/SemanticJavaDebugger) (sjdb).
For instance, the code in [`src/main/kotlin/de/ahbnr/semanticweb/sjdb/repl/commands/BuildKBCommand.kt`](https://github.com/ahbnr/SemanticJavaDebugger/blob/main/src/main/kotlin/de/ahbnr/semanticweb/sjdb/repl/commands/BuildKBCommand.kt) in the `sjdb` project constructs a knowledge base from the state of a paused Java program.
The other files in that directory also contain many examples on how that knowledge base can be queried.

Furthermore, there are some examples on how to use the plugin system of `jdi2owl` for extending a mapping.
[`MappingPluginTest.kt`](https://github.com/ahbnr/jdi2owl/blob/main/src/test/kotlin/de/ahbnr/semanticweb/jdi2owl/tests/MappingPluginTest.kt) uses a simple dummy plugin that just inserts a single "dummy" individual into a knowledge base during the mapping.
The Semantic Java Debugger [contains a plugin](https://github.com/ahbnr/SemanticJavaDebugger/tree/main/src/main/kotlin/de/ahbnr/semanticweb/sjdb/mapping/forward/extensions/sourceinfo) that extends the mapping with information from the source code of a Java program by utilizing [Spoon](https://spoon.gforge.inria.fr/).
I.e. it annotates methods with their declaration location.

## License :balance_scale:

See [LICENSE.txt](./LICENSE.txt).

