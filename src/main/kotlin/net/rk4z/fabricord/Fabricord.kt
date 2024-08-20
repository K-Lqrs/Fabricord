package net.rk4z.fabricord

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.rk4z.fabricord.discord.DiscordBotManager
import net.rk4z.fabricord.discord.DiscordEmbed
import net.rk4z.fabricord.discord.DiscordPlayerEventHandler.handleMCMessage
import net.rk4z.fabricord.utils.Utils.copyResourceToFile
import net.rk4z.fabricord.utils.Utils.getNullableBoolean
import net.rk4z.fabricord.utils.Utils.getNullableString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import kotlin.io.path.notExists

object Fabricord : DedicatedServerModInitializer {
	const val MOD_ID = "fabricord"

	val logger: Logger = LoggerFactory.getLogger(Fabricord::class.simpleName)

	private val loader: FabricLoader = FabricLoader.getInstance()
	val executorService = Executors.newSingleThreadExecutor()

	private val serverDir: Path = loader.gameDir.toRealPath()
	val modDir: Path = serverDir.resolve(MOD_ID)
	val configFile: Path = modDir.resolve("config.yml")

	private val yaml = Yaml()
	private var initializeIsDone = false

	//region Configurations
	// Required
	var botToken: String? = null
	var logChannelID: String? = null

	// Optional
	var enableConsoleLog: Boolean? = false
	var consoleLogChannelID: String? = null

	var serverStartMessage: String? = null
	var serverStopMessage: String? = null
	var playerJoinMessage: String? = null
	var playerLeaveMessage: String? = null

	var botOnlineStatus: String? = null
	var botActivityStatus: String? = null
	var botActivityMessage: String? = null

	var messageStyle: String? = null
	var webHookUrl: String? = null
	//endregion

	override fun onInitializeServer() {
		logger.info("Initializing Fabricord...")
		checkRequiredFilesAndDirectories()
		loadConfig()

		if (requiredNullCheck()) {
			logger.error("Bot token or log channel ID is missing in config file.")
			logger.error("Maybe you are running the mod for the first time?")
			logger.error("Please check the config file at $configFile")
			return
		}
		nullCheck()

		registerPlayerEvents()
		registerServerLifecycleEvents()

		initializeIsDone = true
		logger.info("Fabricord initialized successfully.")
	}

	private fun registerServerLifecycleEvents() {
		ServerLifecycleEvents.SERVER_STARTED.register { server ->
			executorService.submit {
				try {
					DiscordBotManager.init(server)
					DiscordBotManager.startBot()
				} catch (e: Exception) {
					logger.error("Failed to start Discord bot", e)
					server.stop(false)
				}
			}
		}

		ServerLifecycleEvents.SERVER_STOPPING.register { server ->
			executorService.submit {
				try {
					DiscordBotManager.stopBot()
					shutdownExecutor()
				} catch (e: Exception) {
					logger.error("Failed to stop Discord bot", e)
				}
			}
		}
	}

	private fun registerPlayerEvents() {
		ServerMessageEvents.CHAT_MESSAGE.register(ServerMessageEvents.ChatMessage { message, sender, _ ->
			val content = message.content.string
			handleMCMessage(sender, content)
		})

		ServerPlayConnectionEvents.JOIN.register(ServerPlayConnectionEvents.Join { handler, _, _ ->
			val player = handler.player
			DiscordEmbed.sendPlayerJoinEmbed(player)
		})

		ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, _ ->
			val player = handler.player
			DiscordEmbed.sendPlayerLeftEmbed(player)
		})
	}

	private fun shutdownExecutor() {
		executorService.shutdown()
	}

	private fun checkRequiredFilesAndDirectories() {
		try {
			if (!Files.exists(modDir)) {
				logger.info("Creating config directory at $modDir")
				Files.createDirectories(modDir)
			}
			if (configFile.notExists()) {
				copyResourceToFile("config.yml", configFile)
			}
		} catch (e: SecurityException) {
			logger.error("Failed to create/check required files or directories due to security restrictions", e)
		} catch (e: IOException) {
			logger.error("Failed to create/check required files or directories due to an I/O error", e)
		} catch (e: Exception) {
			logger.error("An unexpected error occurred while creating/checking required files or directories", e)
		}
	}

	private fun loadConfig() {
		try {
			logger.info("Loading config file...")

			if (Files.notExists(configFile)) {
				logger.error("Config file not found at $configFile")
				return
			}

			Files.newInputStream(configFile).use { inputStream ->
				val config: Map<String, Any> = yaml.load(inputStream)

				// Required
				botToken = config.getNullableString("BotToken")
				logChannelID = config.getNullableString("LogChannelID")

				// this feature is not supported in the current version
				enableConsoleLog = config.getNullableBoolean("EnableConsoleLog")
				consoleLogChannelID = config.getNullableString("ConsoleLogChannelID")

				// Optional
				serverStartMessage = config.getNullableString("ServerStartMessage")
				serverStopMessage = config.getNullableString("ServerStopMessage")
				playerJoinMessage = config.getNullableString("PlayerJoinMessage")
				playerLeaveMessage = config.getNullableString("PlayerLeaveMessage")

				botOnlineStatus = config.getNullableString("BotOnlineStatus")
				botActivityStatus = config.getNullableString("BotActivityStatus")
				botActivityMessage = config.getNullableString("BotActivityMessage")

				messageStyle = config.getNullableString("MessageStyle")
				webHookUrl = config.getNullableString("WebHookUrl")
			}
		} catch (e: IOException) {
			logger.error("Failed to load config file", e)
		} catch (e: Exception) {
			logger.error("An unexpected error occurred while loading config:", e)
		}
	}

	private fun nullCheck() {
		if (botActivityMessage.isNullOrBlank()) {
			botActivityMessage = "Minecraft Server"
		}
		if (botActivityStatus.isNullOrBlank()) {
			botActivityStatus = "playing"
		}
		if (botOnlineStatus.isNullOrBlank()) {
			botOnlineStatus = "online"
		}
		if (messageStyle.isNullOrBlank()) {
			messageStyle = "classic"
		}
		if (serverStartMessage.isNullOrBlank()) {
			serverStartMessage = ":white_check_mark: **Server has started!**"
		}
		if (serverStopMessage.isNullOrBlank()) {
			serverStopMessage = ":octagonal_sign: **Server has stopped!**"
		}
	}

	private fun requiredNullCheck(): Boolean {
		return botToken.isNullOrBlank() || logChannelID.isNullOrBlank()
	}
}