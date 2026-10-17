plugins {
    id("fabric-loom") version "1.6-SNAPSHOT"
    `maven-publish`
}

val minecraft_version = "1.16.5"
val yarn_mappings = "1.16.5+build.10:v2"
val loader_version = "0.15.11"

dependencies {
    minecraft("com.mojang:minecraft:${minecraft_version}")
    mappings("net.fabricmc:yarn:${yarn_mappings}")
    modImplementation("net.fabricmc:fabric-loader:${loader_version}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
