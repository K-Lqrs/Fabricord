package net.ririfa.fabricord

import net.ririfa.fabricord.annotations.Required
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.util.copyResourceToFile
import net.ririfa.fabricord.util.extractWebhookIdFromUrl
import net.ririfa.fabricord.util.toBooleanOrNull
import org.yaml.snakeyaml.Yaml
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.notExists
import kotlin.reflect.full.memberProperties

object ConfigManager {
	lateinit var parsedConfig: Map<String, Any>
	lateinit var config: Config
	val yaml = Yaml()
	val configFile: Path = ModDir.resolve("config.yml")
	var isErrorOccurred = false

	fun init() {
		checkRequiredFilesAndDirectories()
		reloadConfig()
		loadConfig()
		validate()
	}

	inline fun <reified T> lc(key: String): T? {
		val value = resolveNestedKey(parsedConfig, key)
		return parseValue(value)
	}

	fun reloadConfig(force: Boolean = false) {
		if (force || !::parsedConfig.isInitialized) {
			parsedConfig = Files.newInputStream(configFile).use { yaml.load(it) }
		}
	}

	// >==================== Helpers ====================< \\

	private fun validate() {
		checkRequiredConfig()
		config.nullCheck()
	}

	private fun checkRequiredFilesAndDirectories() {
		try {
			if (!Files.exists(ModDir)) {
				Logger.info(LM.getSysMessage(FabricordMessageKey.System.Initialization.DirectoriesAndFiles.ModDirDoesNotExist, ModDir))
				Files.createDirectories(ModDir)
			}
			if (configFile.notExists()) {
				copyResourceToFile("config.yml", configFile)
			}
		} catch (e: SecurityException) {
			Logger.error(LM.getSysMessage(TODO()), e)
		} catch (e: IOException) {
			Logger.error(LM.getSysMessage(TODO()), e)
		} catch (e: Exception) {
			Logger.error(LM.getSysMessage(TODO()), e)
		}
	}

	fun resolveNestedKey(config: Map<String, Any>, key: String): Any? {
		val keys = key.split(".") // "main.example" -> ["main", "example"]
		var current: Any? = config

		for (part in keys) {
			if (current !is Map<*, *>) {
				return null // 現在のノードがマップでない場合、探索を中止
			}
			current = current[part]
		}

		return current
	}

	inline fun <reified T> parseValue(value: Any?): T? {
		return when (T::class) {
			String::class -> value?.toString() as? T
			Int::class -> value?.toString()?.toIntOrNull() as? T
			Boolean::class -> value?.toString()?.toBooleanOrNull() as? T
			Double::class -> value?.toString()?.toDoubleOrNull() as? T
			Short::class -> value?.toString()?.toShortOrNull() as? T
			Long::class -> value?.toString()?.toLongOrNull() as? T
			Float::class -> value?.toString()?.toFloatOrNull() as? T
			Byte::class -> value?.toString()?.toByteOrNull() as? T
			Char::class -> (value as? String)?.singleOrNull() as? T
			BigInteger::class -> value?.toString()?.let { BigInteger(it) } as? T
			BigDecimal::class -> value?.toString()?.let { BigDecimal(it) } as? T
			List::class -> (value as? List<*>)?.filterIsInstance<T>() as? T
			Set::class -> (value as? List<*>)?.filterIsInstance<T>()?.toSet() as? T
			else -> value as? T
		}
	}

	private fun checkRequiredConfig() {
		val clazz = config::class
		val properties = clazz.memberProperties

		Logger.debug("Found ${properties.size} properties in Config class.")

		for (property in properties) {
			if (property.annotations.any { it is Required }) {
				val value = property.getter.call(config) as? String
				if (value.isNullOrBlank()) {
					Logger.error(LM.getSysMessage(FabricordMessageKey.Exception.Config.RequiredPropertyIsNotConfigured, configFile, property.name))
					isErrorOccurred = true
				}
			}
		}
	}

	private fun loadConfig() {
		try {
			if (!::parsedConfig.isInitialized) {
				throw IllegalStateException("parsedConfig is not initialized. Call reloadConfig() first.")
			}

			config = Config(
				botToken = lc<String>("bot.token") ?: "",
				logChannelID = lc<String>("bot.logChannelID") ?: "",
				botActivityMessage = lc("bot.activityMessage"),
				botActivityStatus = lc("bot.activityStatus"),
				botOnlineStatus = lc("bot.onlineStatus"),
				messageStyle = lc("bot.messageStyle"),
				webHookId = extractWebhookIdFromUrl(lc("bot.webHookUrl")),
				serverStartMessage = lc("messages.serverStart"),
				serverStopMessage = lc("messages.serverStop"),
				playerJoinMessage = lc("messages.playerJoin"),
				playerLeaveMessage = lc("messages.playerLeave"),
				allowMentions = lc("mentions.allowMentions"),
				useUserPermissionForMentions = lc("mentions.useUserPermissionForMentions"),
				mentionBlockedUserID = lc("mentions.blockedUserIDs"),
				mentionBlockedRoleID = lc("mentions.blockedRoleIDs"),
				enableConsoleLog = lc("logging.enableConsoleLog"),
				consoleLogChannelID = lc("logging.consoleLogChannelID")
			)
		} catch (e: Exception) {
			Logger.error("Failed to load config: ${e.message}", e)
		}
	}

	// >================================================< \\
	data class Config(
		@Required val botToken: String,
		@Required val logChannelID: String,

		var botActivityMessage: String? = null,
		var botActivityStatus: String? = null,
		var botOnlineStatus: String? = null,
		var messageStyle: String? = null,
		val webHookId: String? = null,
		var serverStartMessage: String? = null,
		var serverStopMessage: String? = null,
		var playerJoinMessage: String? = null,
		var playerLeaveMessage: String? = null,

		var allowMentions: Boolean? = true,
		var useUserPermissionForMentions: Boolean? = false,
		var mentionBlockedUserID: Set<String>? = emptySet(),
		var mentionBlockedRoleID: Set<String>? = emptySet(),

		var enableConsoleLog: Boolean? = true,
		var consoleLogChannelID: String? = null,
	) {
		fun nullCheck() {
			if (botActivityMessage.isNullOrBlank()) botActivityMessage = "Minecraft Server"
			if (botActivityStatus.isNullOrBlank()) botActivityStatus = "playing"
			if (botOnlineStatus.isNullOrBlank()) botOnlineStatus = "online"
			if (messageStyle.isNullOrBlank()) messageStyle = "classic"
			if (serverStartMessage.isNullOrBlank()) serverStartMessage = ":white_check_mark: **Server has started!**"
			if (serverStopMessage.isNullOrBlank()) serverStopMessage = ":octagonal_sign: **Server has stopped!**"
			if (playerJoinMessage.isNullOrBlank()) playerJoinMessage = "%player% joined the server"
			if (playerLeaveMessage.isNullOrBlank()) playerLeaveMessage = "%player% left the server"

			if (allowMentions == null) allowMentions = true
			if (useUserPermissionForMentions == null) useUserPermissionForMentions = false
			if (mentionBlockedUserID == null) mentionBlockedUserID = emptySet()
			if (mentionBlockedRoleID == null) mentionBlockedRoleID = emptySet()

			if (enableConsoleLog == null) enableConsoleLog = true
			if (consoleLogChannelID.isNullOrBlank()) consoleLogChannelID = "0"
		}
	}
}