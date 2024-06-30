import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("fabric-loom")
}

group = "net.rk4z.fabricord"
version = "4.0-1.21"

repositories {
    mavenCentral()
}

val includeInJar: Configuration by configurations.creating

dependencies {
    val minecraftVersion: String by project
    val mappingsVersion: String by project
    val loaderVersion: String by project
    val fabricVersion: String by project

    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$mappingsVersion")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")

    implementation("net.rk4z:beacon:1.2.5")
    implementation("net.dv8tion:JDA:5.0.0-beta.24"){
        exclude("net.java.dev.jna", "jna")
    }

    implementation("org.yaml:snakeyaml:2.0")
    implementation("club.minnced:discord-webhooks:0.8.4")
    implementation("net.kyori:adventure-text-serializer-gson:4.14.0")

    includeInJar("net.rk4z:beacon:1.2.5")
    includeInJar("net.dv8tion:JDA:5.0.0-beta.24") {
        exclude("net.java.dev.jna", "jna")
    }
    includeInJar("org.yaml:snakeyaml:2.0")
    includeInJar("club.minnced:discord-webhooks:0.8.4")
    includeInJar("net.kyori:adventure-text-serializer-gson:4.14.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
tasks.withType<JavaCompile> {
    options.release.set(21)
}
tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    archiveFileName.set("${project.name}-${project.version}.jar")
    archiveClassifier = ""

    from("LICENSE") {
        rename { "${it}_${project.name}" }
    }

    from({
        configurations["includeInJar"].map { project.zipTree(it) }
    })
}