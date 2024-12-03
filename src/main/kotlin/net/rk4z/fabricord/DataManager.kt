package net.rk4z.fabricord

import net.minecraft.server.network.ServerPlayerEntity
import java.nio.file.Path

class DataManager {
    val file: Path = Fabricord.get()?.dataFolder?.resolve("linkdata.json") ?: error("Data file not found")
    val data: MutableList<LinkData> = mutableListOf()

    fun createJsonIfNeeded() {
        if (!file.toFile().exists()) {
            file.toFile().createNewFile()
        }
    }

    fun insertLinkData(playerUUID: String, discordID: String) {
        data.add(LinkData(playerUUID, discordID))
    }

    fun isPlayerLinkedWithDiscord(playerUUID: String): Boolean {
        return data.any { it.playerUUID == playerUUID }
    }

    fun getDiscordID(playerUUID: String): String {
        return data.first { it.playerUUID == playerUUID }.discordID
    }
}

data class LinkData(
    val playerUUID: String,
    val discordID: String
)

data class LinkData(
    val playerUUID: String,
    val discordID: String
)

data class PlayerData(

)

fun ServerPlayerEntity.isLinkedWithDiscord(): Boolean {
    return DataManager().isPlayerLinkedWithDiscord(this.uuidAsString)
}

fun ServerPlayerEntity.getDiscordID(): String {
    return DataManager().getDiscordID(this.uuidAsString)
}

fun ServerPlayerEntity.linkWithDiscord(discordID: String) {
    DataManager().insertLinkData(this.uuidAsString, discordID)
}
