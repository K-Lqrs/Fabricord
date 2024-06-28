package net.rk4z.fabricord.util

import net.rk4z.fabricord.Fabricord.logger
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

object Utils {
    /**
     * Retrieves a nullable string value from the map associated with the given key.
     * Returns null if the value is null, or if the value is a blank string.
     *
     * @param key The key used to retrieve the value from the map.
     * @return The nullable string value associated with the given key, or null if the value is null or blank.
     */
    private fun Map<String, Any>.getNullableString(key: String): String? =
        this[key]?.toString()?.takeIf { it.isNotBlank() }

    fun copyResourceToFile(resourcePath: String, outputPath: Path) {
        val inputStream: InputStream? = javaClass.getResourceAsStream(resourcePath)
        if (inputStream == null) {
            logger.error("Resource $resourcePath not found in Jar")
            return
        }
        Files.copy(inputStream, outputPath)
        logger.info("Copied resource $resourcePath to $outputPath")
    }
}