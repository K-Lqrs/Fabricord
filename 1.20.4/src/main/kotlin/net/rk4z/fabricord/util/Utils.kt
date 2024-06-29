package net.rk4z.fabricord.util

import net.minecraft.server.network.ServerPlayerEntity
import net.rk4z.fabricord.Fabricord.logger
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

object Utils {
    fun Map<String, Any>.getNullableString(key: String): String? =
        this[key]?.toString()?.takeIf { it.isNotBlank() }

    fun Map<String, Any>.getNullableBoolean(key: String): Boolean? =
        this[key]?.toString()?.takeIf { it.isNotBlank() }?.toBooleanStrictOrNull()



    fun copyResourceToFile(resourcePath: String, outputPath: Path) {
        val inputStream: InputStream? = javaClass.getResourceAsStream(resourcePath)
        if (inputStream == null) {
            logger.error("Resource $resourcePath not found in Jar")
            return
        }
        Files.copy(inputStream, outputPath)
        logger.info("Copied resource $resourcePath to $outputPath")
    }

    fun replaceUUIDsWithMCIDs(message: String, players: List<ServerPlayerEntity>): Pair<String, List<ServerPlayerEntity>> {
        var updatedMessage = message
        val mentionedPlayers = mutableListOf<ServerPlayerEntity>()
        val uuidPattern = Regex("@\\{([0-9a-fA-F-]+)}")
        val matches = uuidPattern.findAll(message)

        matches.forEach { match ->
            val uuidStr = match.groupValues[1]
            val player = players.find { it.uuid.toString() == uuidStr }
            player?.let {
                updatedMessage = updatedMessage.replace(match.value, "@${it.name.string}")
                mentionedPlayers.add(it)
            }
        }
        return Pair(updatedMessage, mentionedPlayers)
    }

}