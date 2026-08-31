import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.10"
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.warasugi"
version = "2.0.0"
description = "Real-time Arabic chat translation for Paper servers"

val javaVersion: String by project
val paperApiVersion: String by project
val apiVersion: String by project
val coroutinesVersion: String by project
val gsonVersion: String by project
val junitVersion: String by project

kotlin {
    jvmToolchain(javaVersion.toInt())
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(javaVersion)
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion.toInt())
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    testCompileOnly("io.papermc.paper:paper-api:$paperApiVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")

    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    processResources {
        val props = mapOf(
            "version" to project.version.toString(),
            "apiVersion" to apiVersion,
            "description" to project.description.toString(),
        )
        inputs.properties(props)
        filesMatching("paper-plugin.yml") { expand(props) }
    }

    test {
        useJUnitPlatform()
        testLogging { events("failed", "skipped") }
    }

    withType<ShadowJar>().configureEach {
        archiveClassifier = ""
        // Minecraft ships its own (older) Gson on the server classpath; relocating
        // keeps us on ours no matter what the server or another plugin loaded.
        relocate("com.google.gson", "com.warasugi.arabictranslator.libs.gson")
        mergeServiceFiles()
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/maven/**")
        exclude("DebugProbesKt.bin")
    }

    build { dependsOn(shadowJar) }

    jar { archiveClassifier = "unshaded" }
}
