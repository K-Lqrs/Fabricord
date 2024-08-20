import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.jetbrains.kotlin.jvm")
	id("fabric-loom")
	id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "net.rk4z.fabricord"
version = "4.0.0"

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
	val beaconVersion: String by project

	minecraft("com.mojang:minecraft:$minecraftVersion")
	mappings("net.fabricmc:yarn:$mappingsVersion")
	modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
	modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")
	modImplementation("net.fabricmc:fabric-language-kotlin:$fabricLanguageKotlinVersion")

	implementation("net.dv8tion:JDA:5.0.1"){
		exclude("net.java.dev.jna", "jna")
	}
	
	implementation("org.yaml:snakeyaml:2.0")
	implementation("club.minnced:discord-webhooks:0.8.4")
	implementation("net.kyori:adventure-text-serializer-gson:4.14.0")

}

fabricApi {
	configureDataGeneration()
}

val targetJavaVersion = 21
java {
	val javaVersion = JavaVersion.toVersion(targetJavaVersion)
	sourceCompatibility = javaVersion
	targetCompatibility = javaVersion
	if (JavaVersion.current() < javaVersion) {
		toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
	}
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"

	if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
		options.release.set(targetJavaVersion)
	}
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

tasks.withType<ShadowJar> {
	archiveFileName.set("${project.name}-${project.version}.jar")
	mergeServiceFiles()
	exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	from("LICENSE") {
		rename { "${it}_${project.name}" }
	}
}