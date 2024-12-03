package net.rk4z.fabricord

import net.dv8tion.jda.api.entities.Guild
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.text.Text
import net.rk4z.fabricord.discord.DiscordBotManager
import net.rk4z.fabricord.discord.DiscordEmbed
import net.rk4z.fabricord.discord.DiscordPlayerEventHandler.handleMCMessage
import net.rk4z.fabricord.utils.Main
import net.rk4z.fabricord.utils.System
import net.rk4z.s1.swiftbase.core.CB
import net.rk4z.s1.swiftbase.core.LMB
import net.rk4z.s1.swiftbase.core.Logger
import net.rk4z.s1.swiftbase.fabric.DedicatedServerModEntry
import net.rk4z.s1.swiftbase.fabric.adapt
import org.slf4j.LoggerFactory

class Fabricord : DedicatedServerModEntry(
	id= "fabricord",
	packageName = "net.rk4z.fabricord",
	isDebug = true,
	configFile = "assets/fabricord/config/en.yml",
	availableLang = listOf("ja", "en"),
	langDir = "lang",
	logger = LoggerFactory.getLogger(Fabricord::class.simpleName),
	enableUpdateChecker = true,
	modrinthID = "xU8Bn98V",
) {
	companion object {
		//region Configurations
		// Required
		var guildId: String? = null
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

		var forceLink: Boolean? = false
		var allowMention: Boolean? = false
		var allowEveryone: Boolean? = false
		var allowHere: Boolean? = false
		var allowRoleMention: Boolean? = false
		var allowedRoles: List<String>? = null
		var allowUserMention: Boolean? = false
		var allowedUsers: List<String>? = null

		var webHookId: String? = null
		//endregion

		fun get(): Fabricord? {
			return get<Fabricord>()
		}
	}

	val name: String = description.name
	val version: String = description.version.friendlyString

	override fun onInstanceInitialized() {
		CB.apply {
			onCheckUpdate = {
				Logger.info(LMB.getSysMessage(System.Log.CHECKING_UPDATE))
			}

			onAllVersionsRetrieved = { versionCount ->
				Logger.info(LMB.getSysMessage(System.Log.ALL_VERSION_COUNT, versionCount.toString()))
			}

			onNewVersionFound = { latestVersion, newerVersionCount ->
				Logger.info(LMB.getSysMessage(System.Log.NEW_VERSION_COUNT, newerVersionCount.toString()))
				Logger.info(LMB.getSysMessage(System.Log.LATEST_VERSION_FOUND, latestVersion, version))
				Logger.info(LMB.getSysMessage(System.Log.VIEW_LATEST_VER, MODRINTH_DOWNLOAD_URL))
			}

			onNoNewVersionFound = {
				Logger.info(LMB.getSysMessage(System.Log.YOU_ARE_USING_LATEST))
			}

			onUpdateCheckFailed = { responseCode ->
				Logger.warn(LMB.getSysMessage(System.Log.FAILED_TO_CHECK_UPDATE, responseCode.toString()))
			}

			onUpdateCheckError = { e ->
				Logger.error(LMB.getSysMessage(System.Log.ERROR_WHILE_CHECKING_UPDATE, e.message ?: LMB.getSysMessage(System.Log.Other.UNKNOWN_ERROR)))
			}
		}
	}

	override fun onDirectoriesAndFilesInitialized() {
		Logger.info(LMB.getSysMessage(System.Log.LOADING, name, version))
		loadConfig()

		if (requiredNullCheck()) {
			Logger.error(LMB.getSysMessage(System.Log.MissingRequiredParam.ITEM_0))
			Logger.error(LMB.getSysMessage(System.Log.MissingRequiredParam.ITEM_1))
			Logger.error(LMB.getSysMessage(System.Log.MissingRequiredParam.ITEM_2, "$configFile"))
			return
		}
		nullCheck()

		registerEvents()

		Logger.info(LMB.getSysMessage(System.Log.INITIALIZED, name))
	}

	override fun onInitialized() {
		if (isDebug) {
			availableLang?.forEach { t ->
				LMB.findMissingKeys(t)
			}
		}
	}

	private fun registerEvents() {
		ServerMessageEvents.CHAT_MESSAGE.register(ServerMessageEvents.ChatMessage { message, sender, _ ->
			CB.executor.executeAsync {
				val content = message.content.string
				handleMCMessage(sender, content)
			}
		})

		ServerPlayConnectionEvents.JOIN.register(ServerPlayConnectionEvents.Join { handler, _, _ ->
			val player = handler.player

			if (!DiscordBotManager.botIsInitialized) {
				player.networkHandler.disconnect(Text.of(LMB.getSysMessage(System.Log.STILL_STARTING_UP)))
				return@Join
			}

			val p = player.adapt()

			if (!(player.isLinkedWithDiscord())) {
				player.networkHandler.disconnect(p.getMessage(Main.NOT_LINKED))
				return@Join
			}

			CB.executor.executeAsync {
				DiscordEmbed.sendPlayerJoinEmbed(player)
			}
		})

		ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, _ ->
			CB.executor.executeAsync {
				val player = handler.player
				DiscordEmbed.sendPlayerLeftEmbed(player)
			}
		})

		ServerLifecycleEvents.SERVER_STARTED.register { server ->
			CB.executor.executeAsync {
				try {
					DiscordBotManager.init(server)
					DiscordBotManager.startBot()
				} catch (e: Exception) {
					logger.error(LMB.getSysMessage(System.Log.FAILED_TO_START, e))
					server.stop(false)
				}
			}
		}

		ServerLifecycleEvents.SERVER_STOPPING.register { _ ->
			try {
				DiscordBotManager.stopBot()
			} catch (e: Exception) {
				logger.error(LMB.getSysMessage(System.Log.FAILED_TO_STOP, e))
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

		forceLink = lc("ForceLink")
		allowMention = lc("AllowedMentions.AllowMention")
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