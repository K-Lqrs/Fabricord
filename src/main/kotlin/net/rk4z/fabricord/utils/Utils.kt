package net.rk4z.fabricord.utils

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import net.minecraft.server.network.ServerPlayerEntity
import net.rk4z.s1.swiftbase.core.Logger
import net.rk4z.s1.swiftbase.core.logIfDebug
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

object Utils {
    // LinkCode + Shortened Player UUID
    val linkCodeCache: Cache<String, String> = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(500)
        .build()

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

    fun generateLinkCode(uuid: ShortUUID, length: Int = 6): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = SecureRandom()
        val shortened = uuid.toShortString()
        val linkCode = (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")

        // ShortUUIDStringをPut
        linkCodeCache.put(linkCode, shortened)
        Logger.logIfDebug("Generated link code: $linkCode for UUID: $shortened")
        return linkCode
    }
}