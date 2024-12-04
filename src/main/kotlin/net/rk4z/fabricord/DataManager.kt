package net.rk4z.fabricord

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import net.minecraft.server.network.ServerPlayerEntity
import net.rk4z.fabricord.utils.ShortUUID
import java.nio.file.Path

object DataManager {
    val file: Path = Fabricord.get()?.dataFolder?.resolve("data.json") ?: error("Data file not found")
    val gson = Gson()
    val data: MutableList<JsonData> = mutableListOf()

    fun createJsonIfNeeded() {
        if (!file.toFile().exists()) {
            file.toFile().createNewFile()
        }
    }

    // ShortUUIDStringを受け取り、保存
    fun insertLinkData(playerUUID: String, discordID: String) {
        createJsonIfNeeded()
        val jsonData = loadJsonData()
        val updatedLinkData = jsonData.linkData.toMutableList().apply {
            removeIf { it.playerUUID == playerUUID }
            add(LinkData(playerUUID, discordID))
        }
        saveJsonData(JsonData(updatedLinkData, jsonData.playerData))
    }

    fun insertPlayerData(playerUUID: String, playerName: String, lang: String) {
        createJsonIfNeeded()
        val jsonData = loadJsonData()
        val updatedPlayerData = jsonData.playerData.toMutableList().apply {
            removeIf { it.uuid == playerUUID }
            add(PlayerData(playerUUID, playerName, lang))
        }
        saveJsonData(JsonData(jsonData.linkData, updatedPlayerData))
    }

    fun isPlayerLinkedWithDiscord(playerUUID: String): Boolean {
        createJsonIfNeeded()
        val shortenedUUID = ShortUUID.fromUUID(playerUUID).toShortString()
        val jsonData = loadJsonData()
        return jsonData.linkData.any { it.playerUUID == shortenedUUID }
    }

    fun isPlayerDataExists(playerUUID: String): Boolean {
        createJsonIfNeeded()
        val jsonData = loadJsonData()
        return jsonData.playerData.any { it.uuid == playerUUID }
    }

    fun getLinkedData(discordID: String): LinkData {
        createJsonIfNeeded()
        val jsonData = loadJsonData()
        return jsonData.linkData.find { it.discordID == discordID }
            ?: error("Discord ID $discordID is not found")
    }

    fun getPlayerData(playerUUID: String): PlayerData {
        createJsonIfNeeded()
        val jsonData = loadJsonData()
        return jsonData.playerData.find { it.uuid == playerUUID }
            ?: error("Player with UUID $playerUUID is not found")
    }

    private fun loadJsonData(): JsonData {
        if (!file.toFile().exists()) return JsonData(emptyList(), emptyList())
        val json = file.toFile().readText()
        return if (json.isBlank()) JsonData(emptyList(), emptyList())
        else gson.fromJson(json, object : TypeToken<JsonData>() {}.type)
    }

    private fun saveJsonData(jsonData: JsonData) {
        file.toFile().writeText(gson.toJson(jsonData))
    }
}

data class JsonData(
    val linkData: List<LinkData>,
    val playerData: List<PlayerData>
)

data class LinkData(
    val playerUUID: String,
    val discordID: String
)

data class PlayerData(
    val uuid: String,
    val name: String,
    val lang: String
)

fun ServerPlayerEntity.isLinkedWithDiscord(): Boolean {
    return DataManager.isPlayerLinkedWithDiscord(this.uuidAsString)
}

fun ServerPlayerEntity.isPlayerDataExists(): Boolean {
    return DataManager.isPlayerDataExists(this.uuidAsString)
}

fun ServerPlayerEntity.getDiscordID(): String {
    return DataManager.getLinkedData(this.uuidAsString).discordID
}