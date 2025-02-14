package net.ririfa.fabricord.util

import net.minecraft.server.network.ServerPlayerEntity
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

fun extractWebhookIdFromUrl(url: String?): String? {
	val regex = Regex("https://discord.com/api/webhooks/([0-9]+)/[a-zA-Z0-9_-]+")
	val matchResult = url?.let { regex.find(it) }
	return matchResult?.groupValues?.get(1)
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