plugins {
    kotlin("jvm") version "1.5.10"
}

group = "de.ahbnr.semanticweb.jdi2owl.plugins"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    // local copy of jdi2owl mapper
    implementation("de.ahbnr.semanticweb:jdi2owl")
}