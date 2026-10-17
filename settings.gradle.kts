pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "StructureRadar"
include("1.16.5")
project(":1.16.5").projectDir = file("versions/1.16.5")