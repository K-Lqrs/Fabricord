package net.rk4z.fabricord

import net.fabricmc.api.EnvType
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.io.path.notExists

class Fabricord : ModInitializer {
	companion object {
		private const val MOD_ID = "fabricord"

		val logger: Logger = LoggerFactory.getLogger(Fabricord::class.simpleName)

		private val loader: FabricLoader = FabricLoader.getInstance()
		val executorService: ExecutorService = Executors.newSingleThreadExecutor()

		private val serverDir: Path = loader.gameDir.toRealPath()
		private val modDir: Path = serverDir.resolve(MOD_ID)
		private val configFile: Path = modDir.resolve("config.yml")

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
	}

	override fun onInitialize() {
		logger.info("Initializing Fabricord...")
		if (loader.environmentType == EnvType.CLIENT) {
			logger.error("Fabricord is a server-side mod and should not be installed on the client side.")
			return
		}

		checkRequiredFilesAndDirectories()
		loadConfig()

		if (requiredNullCheck()) {
			logger.error("Bot token or log channel ID is missing in config file.")
			logger.error("Maybe you are running the mod for the first time?")
			logger.error("Please check the config file at $configFile")
			return
		}
		nullCheck()

		registerEvents()

		initializeIsDone = true
		logger.info("Fabricord initialized successfully.")
	}

	private fun registerEvents() {
		ServerMessageEvents.CHAT_MESSAGE.register(ServerMessageEvents.ChatMessage { message, sender, _ ->
			executorService.submit {
				val content = message.content.string
				handleMCMessage(sender, content)
			}
		})

		ServerPlayConnectionEvents.JOIN.register(ServerPlayConnectionEvents.Join { handler, _, _ ->
			val player = handler.player

			if (!DiscordBotManager.botIsInitialized) {
				player.networkHandler.disconnect(Text.of("Server is still starting up, please try again later."))
				ActionResult.FAIL
			} else {
				ActionResult.SUCCESS
			}

			executorService.submit {
				DiscordEmbed.sendPlayerJoinEmbed(player)
			}
		})

		ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, _ ->
			executorService.submit {
				val player = handler.player
				DiscordEmbed.sendPlayerLeftEmbed(player)
			}
		})

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

		ServerLifecycleEvents.SERVER_STOPPING.register { _ ->
			try {
				DiscordBotManager.stopBot()
				executorService.shutdown()
			} catch (e: Exception) {
				logger.error("Failed to stop Discord bot", e)
			}
		}
	}

//>------------------------------------------------------------------------------------------------------------------<\\

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