rootProject.name = "ArabicTranslator"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc"
            content { includeGroup("io.papermc.paper"); includeGroup("com.mojang"); includeGroup("io.papermc") }
        }
    }
}
