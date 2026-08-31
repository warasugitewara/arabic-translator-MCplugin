rootProject.name = "ArabicTranslator"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // Paper API plus the transitive dependencies it pulls (bungeecord-chat,
        // brigadier, adventure) that Central does not host.
        maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
    }
}
