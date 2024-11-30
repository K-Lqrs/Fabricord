package net.rk4z.fabricord

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.text.Text
import net.rk4z.fabricord.discord.DiscordBotManager
import net.rk4z.fabricord.discord.DiscordEmbed
import net.rk4z.fabricord.discord.DiscordPlayerEventHandler.handleMCMessage
import net.rk4z.s1.swiftbase.fabric.DedicatedServerModEntry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

class Fabricord : DedicatedServerModEntry(
	"fabricord",
	"net.rk4z.fabricord",
	false,
	configFile = "config.yml",
	availableLang = listOf("ja", "en"),
	langDir = "lang",
	logger = LoggerFactory.getLogger(Fabricord::class.simpleName),
	enableUpdateChecker = true,
	modrinthID = "xU8Bn98V",
) {
	//TODO: 言語の実装、起動時の呼び出しの実装
	companion object {
		private const val MOD_ID = "fabricord"

		val logger: Logger = LoggerFactory.getLogger(Fabricord::class.simpleName)

		private val loader: FabricLoader = FabricLoader.getInstance()
		val executorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

		private val serverDir: Path = loader.gameDir.toRealPath()
		private val modDir: Path = serverDir.resolve(MOD_ID)

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
		var allowEveryone: Boolean? = false
		var allowHere: Boolean? = false
		var allowRoleMention: Boolean? = false
		var allowedRoles: List<String>? = null
		var allowUserMention: Boolean? = false
		var allowedUsers: List<String>? = null
		var webHookId: String? = null
		//endregion
	}

	override fun onInitializeServer() {
		logger.info("Initializing Fabricord...")
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
				return@Join
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
				executorService.shutdownNow()
			} catch (e: Exception) {
				logger.error("Failed to stop Discord bot", e)
			}
		}
	}

//>------------------------------------------------------------------------------------------------------------------<\\

	private fun loadConfig() {
		// Required
		botToken = lc("BotToken")
		logChannelID = lc("LogChannelID")

		// this feature is not supported in the current version
		enableConsoleLog = lc("EnableConsoleLog")
		consoleLogChannelID = lc("ConsoleLogChannelID")

		// Optional
		serverStartMessage = lc("ServerStartMessage")
		serverStopMessage = lc("ServerStopMessage")
		playerJoinMessage = lc("PlayerJoinMessage")
		playerLeaveMessage = lc("PlayerLeaveMessage")

		botOnlineStatus = lc("BotOnlineStatus")
		botActivityStatus = lc("BotActivityStatus")
		botActivityMessage = lc("BotActivityMessage")

		messageStyle = lc("MessageStyle")

		allowEveryone = lc("AllowedMentions.AllowEveryone")
		allowHere = lc("AllowedMentions.AllowHere")
		allowRoleMention = lc("AllowedMentions.AllowRole")
		allowedRoles = lc("AllowedMentions.AllowedRoles")
		allowUserMention = lc("AllowedMentions.AllowUser")
		allowedUsers = lc("AllowedMentions.AllowedUsers")

		webHookId = extractWebhookIdFromUrl(lc("WebhookUrl"))
	}

	private fun extractWebhookIdFromUrl(url: String?): String? {
		val regex = Regex("https://discord.com/api/webhooks/([0-9]+)/[a-zA-Z0-9_-]+")
		val matchResult = url?.let { regex.find(it) }
		return matchResult?.groupValues?.get(1)
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
		if (playerJoinMessage.isNullOrBlank()) {
			playerJoinMessage = "%player% joined the server"
		}
		if (playerLeaveMessage.isNullOrBlank()) {
			playerLeaveMessage = "%player% left the server"
		}
	}

	private fun requiredNullCheck(): Boolean {
		return botToken.isNullOrBlank() || logChannelID.isNullOrBlank()
	}
}