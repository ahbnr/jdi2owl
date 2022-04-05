import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    implementation("org.apache.jena:apache-jena-libs:4.2.0")

    // Spoon for Java source code analysis
    implementation("fr.inria.gforge.spoon:spoon-core:10.0.1-beta-1")

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
    implementation("io.insert-koin:koin-core:3.1.3")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
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

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}