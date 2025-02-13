package net.ririfa.fabricord.util

import net.ririfa.fabricord.Fabricord
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

fun String.toBooleanOrNull(): Boolean? {
	return when (this.trim().lowercase()) {
		"true", "1", "t" -> true
		"false", "0", "f" -> false
		else -> null
	}
}

fun copyResourceToFile(resourcePath: String, outputPath: Path) {
	val fullPath = "/$resourcePath"
	val inputStream: InputStream? = Fabricord::class.java.getResourceAsStream(fullPath)
	if (inputStream == null) {
		Fabricord.logger.error("Resource $fullPath not found in Jar")
		return
	}
	Files.copy(inputStream, outputPath)
	Fabricord.logger.info("Copied resource $fullPath to $outputPath")
}