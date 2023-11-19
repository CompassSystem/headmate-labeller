plugins {
    kotlin("jvm") version "1.9.20"
    application
}

group = "compass_system.headmate-labeller"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("compass_system.headmate_labeller.MainKt")
}