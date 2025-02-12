package net.ririfa.fabricord.grp

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.ririfa.fabricord.Fabricord
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*

data class PlayerGroupInfo(
	var defaultGroup: String?,
	var currentGroup: String?
)

object PlayerGroupStorage {
	private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
	private val file: Path = Fabricord.modDir.resolve("pgi.json")
	private var data: MutableMap<UUID, PlayerGroupInfo> = mutableMapOf()

	fun load() {
		if (Files.exists(file)) {
			try {
				val json = Files.readString(file)
				data = gson.fromJson(json, object : com.google.gson.reflect.TypeToken<MutableMap<UUID, PlayerGroupInfo>>() {}.type)
			} catch (e: Exception) {
				println("Failed to load player group info: ${e.message}")
			}
		}
	}

	fun save() {
		try {
			val json = gson.toJson(data)
			Files.writeString(file, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
		} catch (e: Exception) {
			println("Failed to save player group info: ${e.message}")
		}
	}

	fun setDefaultGroup(playerId: UUID, groupId: String?) {
		data.computeIfAbsent(playerId) { PlayerGroupInfo(null, null) }.defaultGroup = groupId
		save()
	}

	fun setCurrentGroup(playerId: UUID, groupId: String?) {
		data.computeIfAbsent(playerId) { PlayerGroupInfo(null, null) }.currentGroup = groupId
		save()
	}

	fun getDefaultGroup(playerId: UUID): String? = data[playerId]?.defaultGroup
	fun getCurrentGroup(playerId: UUID): String? = data[playerId]?.currentGroup
}