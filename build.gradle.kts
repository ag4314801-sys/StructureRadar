plugins {
    id("fabric-loom") version "1.17-SNAPSHOT"
    id("maven-publish")
}

val mcVersion = "1.16.5"
version = "${property("mod_version")}+$mcVersion"
group = property("maven_group").toString()

base {
    archivesName.set(property("archives_base_name").toString())
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.terraformersmc.com/releases/")
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
}

stonecutter {
    // Define semantic version constants for preprocessor blocks in Java sources
    swaps["mc_version"] = "\"$mcVersion\""
    const("modern_registries", eval(mcVersion, ">=1.19.3"))
    const("draw_context", eval(mcVersion, ">=1.20.0"))
    const("identifier_of", eval(mcVersion, ">=1.21.0"))
    const("mc_26_2_gui", eval(mcVersion, ">=26.2"))
    const("extended_height", eval(mcVersion, ">=1.17.0"))
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mc_version", mcVersion)

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to project.version,
                "mc_version" to mcVersion
            )
        )
    }
}

val targetJava = when {
    stonecutter.eval(mcVersion, ">=26.1") -> 25
    stonecutter.eval(mcVersion, ">=1.20.5") -> 21
    stonecutter.eval(mcVersion, ">=1.18.0") -> 17
    else -> 16
}

java {
    withSourcesJar()
    val javaVer = JavaVersion.toVersion(targetJava)
    sourceCompatibility = javaVer
    targetCompatibility = javaVer
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJava)
}
