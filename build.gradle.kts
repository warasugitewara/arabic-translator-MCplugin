import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.10"
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.warasugi"
version = "2.0.0"
description = "Real-time chat translation for Paper servers - Arabic transliteration and Chinese Pinyin"

val javaVersion: String by project
val coroutinesVersion: String by project
val gsonVersion: String by project
val pinyin4jVersion: String by project
val junitVersion: String by project

// Paper target, selected with -Ptarget=paper121 | paper26 (see gradle.properties).
val target = providers.gradleProperty("target").getOrElse("paper121")
val paperApiVersion = property("paperApiVersion.$target") as String
val apiVersion = property("apiVersion.$target") as String
val paperLabel = property("label.$target") as String

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
    // Full Unihan reading table, simplified and traditional (BSD licensed).
    implementation("com.belerweb:pinyin4j:$pinyin4jVersion")

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
        // The jar says which Paper line it was built for, so the two cannot be
        // mixed up in a plugins folder.
        archiveClassifier = paperLabel
        // Minecraft ships its own (older) Gson on the server classpath; relocating
        // keeps us on ours no matter what the server or another plugin loaded.
        relocate("com.google.gson", "com.warasugi.arabictranslator.libs.gson")
        relocate("net.sourceforge.pinyin4j", "com.warasugi.arabictranslator.libs.pinyin4j")
        // pinyin4j bundles its own copy of the Sparta XML parser.
        relocate("com.hp.hpl.sparta", "com.warasugi.arabictranslator.libs.sparta")
        mergeServiceFiles()
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/maven/**")
        exclude("DebugProbesKt.bin")
    }

    build { dependsOn(shadowJar) }

    jar { archiveClassifier = "unshaded" }
}
