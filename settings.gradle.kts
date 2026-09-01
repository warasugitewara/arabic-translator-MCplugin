plugins {
    // Lets Gradle fetch the JDK a target needs (Paper 26.x wants Java 25) instead
    // of failing on a machine that only has one JDK installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ArabicTranslator"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // Paper API plus the transitive dependencies it pulls (bungeecord-chat,
        // brigadier, adventure) that Central does not host.
        maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
    }
}
