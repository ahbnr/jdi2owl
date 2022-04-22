import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Paths

plugins {
    // id("com.github.johnrengelman.shadow") version "7.1.0"
    kotlin("jvm") version "1.5.31"
}

group = "de.ahbnr.semanticweb"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    flatDir {
        dirs("../logging/build/libs")
    }
}

dependencies {
    implementation(kotlin("stdlib"))

    // Apache Jena
    api("org.apache.jena:apache-jena-libs:4.2.0")

    // OWL-API 5 to support importing ontologies and parsing non-rdf knowledge bases, as well as for linting
    implementation("net.sourceforge.owlapi:owlapi-api:5.1.19")

    // OntAPI for translating between RDF graph view and ontology view of knowledge base
    implementation("com.github.owlcs:ontapi:3.0.0")

    // Automata for checking values against the allowed value spaces of XSD data types
    implementation("dk.brics.automaton:automaton:1.11-8")

    // Openllet components for OWL and RDF linting
    implementation("com.github.galigator.openllet:openllet-pellint:2.6.5")
    implementation("com.github.galigator.openllet:openllet-jena:2.6.5")

    // Additional data structures / collections
    implementation("org.apache.commons:commons-collections4:4.4")

    // Dependency injection
    api("io.insert-koin:koin-core:3.1.3")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation("org.assertj:assertj-core:3.22.0")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs = listOf(
        "--add-opens", "jdk.jdi/com.sun.tools.jdi=ALL-UNNAMED"
    )

    // Compile the dummy plugin, since we load it during testing to test out the plugin feature
    dependsOn(gradle.includedBuild("DummyPlugin").task(":jar"))
    // put it on the class path, so that we can load it during testing
    classpath = project.sourceSets.test.get().runtimeClasspath +
                    files(
                        Paths.get(
                            gradle.includedBuild("DummyPlugin").projectDir.path,
                            "build/libs",
                            "DummyPlugin-1.0-SNAPSHOT.jar"
                        )
                    )

    // Parallelize tests
    maxParallelForks = Runtime.getRuntime().availableProcessors().div(2)
    if (maxParallelForks < 1) {
        maxParallelForks = 1
    }
}

// tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
//     archiveBaseName.set("jdi2owl")
//     mergeServiceFiles()
// }
//
// tasks {
//     build {
//         dependsOn(shadowJar)
//     }
// }

tasks.withType<JavaCompile>() {
    sourceCompatibility = "11"
    targetCompatibility = "11"

    options.compilerArgs.addAll(
        listOf(
            // This should be supplied to javac to tell it that we may access internal package names at compile time.
            // We circumvent this by using the kotlinc compiler and telling it to ignore these errors by @Suppress annotations in the code
            //
            // Due to these two reasons, this line is probably unnecessary, but we add it for good measure
            "--add-exports", "jdk.jdi/com.sun.tools.jdi=ALL-UNNAMED",
            // See also https://nipafx.dev/five-command-line-options-hack-java-module-system/
        )
    )
}

tasks.withType<KotlinCompile>() {
    sourceCompatibility = "11"
    targetCompatibility = "11"

    kotlinOptions.jvmTarget = "11"
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        )
    }
}