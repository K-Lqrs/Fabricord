import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.jetbrains.kotlin.jvm")
	id("fabric-loom")
}

group = "net.rk4z.fabricord"
version = "3.9.8"

repositories {
	mavenCentral()
}

val includeInJar: Configuration by configurations.creating

dependencies {
	val minecraftVersion: String by project
	val mappingsVersion: String by project
	val loaderVersion: String by project
	val fabricVersion: String by project
	val fabricLanguageKotlinVersion: String by project

	minecraft("com.mojang:minecraft:$minecraftVersion")
	mappings("net.fabricmc:yarn:$mappingsVersion")
	modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
	modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")
	modImplementation("net.fabricmc:fabric-language-kotlin:$fabricLanguageKotlinVersion")

	implementation("net.dv8tion:JDA:5.0.2") {
		exclude("net.java.dev.jna", "jna")
	}

	implementation("org.yaml:snakeyaml:2.0")
	implementation("net.kyori:adventure-text-serializer-gson:4.14.0")

	includeInJar("net.dv8tion:JDA:5.0.2") {
		exclude("net.java.dev.jna", "jna")
	}
	includeInJar("org.yaml:snakeyaml:2.0")
	includeInJar("net.kyori:adventure-text-serializer-gson:4.14.0")
}

val targetJavaVersion = 17

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
	}
	withSourcesJar()
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
	options.release.set(targetJavaVersion)
}

kotlin {
	jvmToolchain {
		languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
	}
}

tasks.withType<KotlinCompile> {
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_17)
	}
}

tasks.named<ProcessResources>("processResources") {
	inputs.property("version", project.version)

	filesMatching("fabric.mod.json") {
		expand(mapOf("version" to project.version))
	}
}

tasks.named("remapSourcesJar") {
	dependsOn(tasks.named("jar"))
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