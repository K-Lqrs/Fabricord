package net.ririfa.fabricord

import net.ririfa.fabricord.annotations.Needed
import net.ririfa.fabricord.util.copyResourceToFile
import net.ririfa.fabricord.util.toBooleanOrNull
import org.yaml.snakeyaml.Yaml
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.notExists

object ConfigManager {
	lateinit var configFile: Path
	lateinit var config: Config
	val yaml = Yaml()

	fun init(config: Config) {
		this.config = config
		checkRequiredFilesAndDirectories()
	}

	fun validate() {
		checkNeededConfig()
		config.nullCheck()
	}

	inline fun <reified T> lc(key: String): T? {
		validateConfigFileProp()

		val config: Map<String, Any> = Files.newInputStream(configFile).use { yaml.load(it) }

		val value = resolveNestedKey(config, key)
		return parseValue(value)
	}

	// >==================== Helpers ====================< \\

	private fun checkRequiredFilesAndDirectories() {
		try {
			if (!Files.exists(ModDir)) {
				Logger.info(LM.getSysMessage(TODO(), ModDir))
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
			String::class -> value as? T
			Int::class -> value?.toString()?.toIntOrNull() as? T
			Boolean::class -> value?.toString()?.toBooleanOrNull() as? T
			Double::class -> value?.toString()?.toDoubleOrNull() as? T
			Short::class -> value?.toString()?.toShortOrNull() as? T
			Long::class -> value?.toString()?.toLongOrNull() as? T
			Float::class -> value?.toString()?.toFloatOrNull() as? T

			// There is a special type! :D
			Byte::class -> value?.toString()?.toByteOrNull() as? T
			Char::class -> (value as? String)?.singleOrNull() as? T
			List::class -> value as? List<*> as? T
			Array<String>::class -> (value as? List<*>)?.filterIsInstance<String>()?.toTypedArray() as? T
			Array<Int>::class -> (value as? List<*>)?.filterIsInstance<Int>()?.toTypedArray() as? T
			Array<Double>::class -> (value as? List<*>)?.filterIsInstance<Double>()?.toTypedArray() as? T
			Map::class -> value as? Map<*, *> as? T
			BigInteger::class -> (value?.toString())?.let { BigInteger(it) } as? T
			BigDecimal::class -> (value?.toString())?.let { BigDecimal(it) } as? T
			else -> value as? T
		}
	}

	fun validateConfigFileProp() {
		if (!::configFile.isInitialized) {
			throw IllegalStateException("Config file is not initialized.")
		}
	}

	private fun checkNeededConfig() {
		val config = config
		val clazz = config::class.java
		val fields = clazz.declaredFields

		for (field in fields) {
			if (field.isAnnotationPresent(Needed::class.java)) {
				field.isAccessible = true
				val value = field.get(config)
				if (value == null) {
					throw IllegalStateException("Field ${field.name} is needed but not initialized.")
				}
			}
		}
	}

	// >================================================< \\
	data class Config(
		@Needed val botToken: String,
		@Needed val logChannelID: String,

		var botActivityMessage: String? = null,
		var botActivityStatus: String? = null,
		var botOnlineStatus: String? = null,
		var messageStyle: String? = null,
		val webHookUrl: String? = null,
		var serverStartMessage: String? = null,
		var serverStopMessage: String? = null,
		var playerJoinMessage: String? = null,
		var playerLeaveMessage: String? = null,
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
		}
	}
}

inline fun <reified T> lc(key: String): T? {
	return ConfigManager.lc<T>(key)
}