package net.rk4z.fabricord.utils

import net.minecraft.server.network.ServerPlayerEntity
import net.rk4z.fabricord.Fabricord
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

object Utils {
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