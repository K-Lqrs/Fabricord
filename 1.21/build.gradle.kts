plugins {
    id("org.jetbrains.kotlin.jvm")
    id("fabric-loom")
}

group = "net.rk4z.fabricord"
version = "4.0-1.21"

repositories {
    mavenCentral()
}

dependencies {
    val minecraftVersion: String by project
    val mappingsVersion: String by project

    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$mappingsVersion")
}