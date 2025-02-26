import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.jetbrains.kotlin.jvm")
	id("fabric-loom")
}

group = "net.ririfa"
version = "4.1.0-alpha"

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

	modApi("net.dv8tion:JDA:5.2.1") {
		exclude("net.java.dev.jna", "jna")
	}
	modApi("org.yaml:snakeyaml:2.3")
	modApi("net.kyori:adventure-text-serializer-gson:4.17.0")
	modApi("net.ririfa:langman:1.4.2")

	modCompileOnly("org.apache.logging.log4j:log4j-api:+")
	modCompileOnly("org.apache.logging.log4j:log4j-core:+")

	includeInJar("net.dv8tion:JDA:5.2.1") {
		exclude("net.java.dev.jna", "jna")
	}
	includeInJar("org.yaml:snakeyaml:2.3")
	includeInJar("net.kyori:adventure-text-serializer-gson:4.17.0")
	includeInJar("net.ririfa:langman:1.4.2")
}

val targetJavaVersion = 21

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
		jvmTarget.set(JvmTarget.JVM_21)
	}
}

tasks.named<ProcessResources>("processResources") {
	inputs.property("version", project.version)

	filesMatching("fabric.mod.json") {
		expand(mapOf("version" to project.version))
	}
}

tasks.named("remapJar") {
	dependsOn(tasks.named("jar"))
	dependsOn(tasks.named("sourcesJar"))
}

tasks.named("remapSourcesJar") {
	dependsOn(tasks.named("sourcesJar"))
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
		configurations["includeInJar"]
			.filter { it.exists() && !it.name.startsWith("kotlin") }
			.map { if (it.isDirectory) it else project.zipTree(it) }
	})
}