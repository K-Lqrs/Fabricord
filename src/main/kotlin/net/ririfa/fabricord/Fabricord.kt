package net.ririfa.fabricord

import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.Text
import net.ririfa.fabricord.discord.DiscordBotManager
import net.ririfa.fabricord.discord.DiscordEmbed
import net.ririfa.fabricord.discord.DiscordPlayerEventHandler
import net.ririfa.fabricord.discord.DiscordPlayerEventHandler.handleMCMessage
import net.ririfa.fabricord.grp.GroupManager
import net.ririfa.fabricord.grp.GroupManager.Commands
import net.ririfa.fabricord.grp.GroupManager.playerInGroupedChat
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.translation.FabricordMessageProvider
import net.ririfa.fabricord.translation.adapt
import net.ririfa.fabricord.utils.Utils.copyResourceToFile
import net.ririfa.fabricord.utils.Utils.getNullableBoolean
import net.ririfa.fabricord.utils.Utils.getNullableString
import net.ririfa.langman.InitType
import net.ririfa.langman.LangMan
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.layout.PatternLayout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.io.path.notExists

class Fabricord : DedicatedServerModInitializer {
	companion object {
		lateinit var instance: Fabricord

		private const val MOD_ID = "fabricord"
		val logger: Logger = LoggerFactory.getLogger(Fabricord::class.simpleName)

		private val loader: FabricLoader = FabricLoader.getInstance()
		val executorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
		internal lateinit var ca: ConsoleTrackerAppender

		private val serverDir: Path = loader.gameDir.toRealPath()
		private val modDir: Path = serverDir.resolve(MOD_ID)
		private val configFile: Path = modDir.resolve("config.yml")
		private val langDir: File = modDir.resolve("lang").toFile()
		private val availableLang = listOf("en", "ja")

		val grpFile: Path = modDir.resolve("groups.json")

		private val yaml = Yaml()
		private var initializeIsDone = false
		private val localChatToggled = mutableListOf<UUID>()

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
		var allowMentions: Boolean? = true
		var webHookId: String? = null
		//endregion
	}

	lateinit var langMan: LangMan<FabricordMessageProvider, Text>
	lateinit var server: MinecraftServer

	override fun onInitializeServer() {
		instance = this
		if (!langDir.exists()) {
			langDir.mkdirs()
		}
		extractLangFiles()

		langMan = LangMan.createNew(
			{ Text.of(it) },
			FabricordMessageKey::class,
			true
		)

		langMan.init(InitType.YAML, langDir, availableLang)

		langMan.messages.forEach { (key, value) ->
			logger.info("Loaded message: $key: $value")
		}

		logger.info(langMan.getSysMessage(FabricordMessageKey.System.Initializing))
		DiscordPlayerEventHandler.init()
		checkRequiredFilesAndDirectories()
		loadConfig()

		if (requiredNullCheck()) {
			logger.error(langMan.getSysMessage(FabricordMessageKey.System.MissingRequiredProp.ITEM1))
			logger.error(langMan.getSysMessage(FabricordMessageKey.System.MissingRequiredProp.ITEM2))
			logger.error(langMan.getSysMessage(FabricordMessageKey.System.MissingRequiredProp.ITEM3, configFile))
			return
		}
		nullCheck()

		registerEvents()

		initializeIsDone = true
		logger.info(langMan.getSysMessage(FabricordMessageKey.System.Initialized))
		if (langMan.isDebug) {
			availableLang.forEach { lang ->
				langMan.logMissingKeys(lang)
			}
		}
	}

	private fun registerEvents() {
		ca = ConsoleTrackerAppender("FabricordConsoleTracker")
		val rootLogger = LogManager.getRootLogger() as org.apache.logging.log4j.core.Logger
		rootLogger.addAppender(ca)

		ServerMessageEvents.CHAT_MESSAGE.register(ServerMessageEvents.ChatMessage { message, sender, params ->
			val uuid = sender.uuid

			if (uuid in localChatToggled || uuid in playerInGroupedChat) return@ChatMessage

			val content = message.content.string
			handleMCMessage(sender, content)
		})

		ServerPlayConnectionEvents.JOIN.register(ServerPlayConnectionEvents.Join { handler, _, _ ->
			val player = handler.player

			if (!DiscordBotManager.botIsInitialized) {
				player.networkHandler.disconnect(
					player.adapt().getMessage(FabricordMessageKey.System.Discord.BotNotInitialized)
				)
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
			GroupManager.initialize()
			executorService.submit {
				try {
					val s = DiscordBotManager.init(server)
					this.server = s
				} catch (e: Exception) {
					logger.error(langMan.getSysMessage(FabricordMessageKey.System.Discord.FailedToStartBot), e)
					server.stop(false)
				}
			}
		}

		ServerLifecycleEvents.SERVER_STOPPING.register { _ ->
			try {
				DiscordBotManager.stopBot()
				executorService.shutdown()
				if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
					executorService.shutdownNow()
				}

				rootLogger.removeAppender(ca)
				ca.stop()
				GroupManager.save()
			} catch (e: Exception) {
				logger.error(langMan.getSysMessage(FabricordMessageKey.System.Discord.FailedToStopBot), e)
			}
		}

		CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
			Commands.entries.forEach { it.register(dispatcher) }

			dispatcher.register(
				literal("lc")
					.executes { context ->
						val player = context.source.player ?: return@executes 0
						val uuid = player.uuid
						val current = uuid in localChatToggled
						val newState = !current
						if (newState) {
							localChatToggled.add(uuid)
						} else {
							localChatToggled.remove(uuid)
						}
						player.sendMessage(
							player.adapt().getMessage(FabricordMessageKey.System.SwitchedLocalChatState, newState),
							false
						)
						return@executes 1
					}
					.then(
						argument("message", greedyString())
							.executes { context ->
								val player = context.source.player ?: return@executes 0
								val message = context.getArgument("message", String::class.java)

								player.server.playerManager.playerList.forEach {
									it.sendMessageToClient(
										Text.of(
											"<${player.name.string}> $message"
										),
										true
									)
								}

								return@executes 1
							}
					)
			)
		}
	}

	internal class ConsoleTrackerAppender(name: String) : AbstractAppender(name, null, PatternLayout.createDefaultLayout(), false, emptyArray()) {
		init {
			start()
		}

		override fun append(event: LogEvent) {
			val rawMessage = event.message.formattedMessage
			val level = event.level

			var safeMessage = rawMessage.replace("```", "`\u200B```")

			if (safeMessage.startsWith("`") || safeMessage.endsWith("`")) {
				safeMessage = "\u200B$safeMessage\u200B"
			}

			val formattedMessage = when (level) {
				Level.INFO, Level.WARN ->

					//
					"""
            ```
            $safeMessage
            ```
            """.trimIndent()
				//

				Level.ERROR -> {
					val errorMessage = safeMessage
						.lineSequence()
						.joinToString("\n") { "- $it" }

					//
					"""
            ```diff
            $errorMessage
            ```
            """.trimIndent()
					//
				}

				else -> return
			}

			DiscordBotManager.sendToDiscordConsole(formattedMessage)
		}
	}

	//>------------------------------------------------------------------------------------------------------------------<\\

	private fun checkRequiredFilesAndDirectories() {
		try {
			if (!Files.exists(modDir)) {
				logger.info(langMan.getSysMessage(FabricordMessageKey.System.CreatingConfigDir, modDir))
				Files.createDirectories(modDir)
			}
			if (configFile.notExists()) {
				copyResourceToFile("config.yml", configFile)
			}
		} catch (e: SecurityException) {
			logger.error(langMan.getSysMessage(FabricordMessageKey.System.FailedToCreateConfigDirBySec), e)
		} catch (e: IOException) {
			logger.error(langMan.getSysMessage(FabricordMessageKey.System.FailedToCreateConfigDirByIO), e)
		} catch (e: Exception) {
			logger.error(langMan.getSysMessage(FabricordMessageKey.System.FailedToCreateConfigDirByUnknown), e)
		}
	}

	private fun extractLangFiles() {
		try {
			val langPath = "assets/fabricord/lang/"
			val classLoader = this::class.java.classLoader

			// It should be not null
			val resourceUrl = classLoader.getResource(langPath)

			val uri = resourceUrl!!.toURI()
			val fs = if (uri.scheme == "jar") FileSystems.newFileSystem(uri, emptyMap<String, Any>()) else null
			val langDirPath = Paths.get(uri)

			Files.walk(langDirPath).use { paths ->
				paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".yml") }.forEach { resourceFile ->
					val targetFile = langDir.resolve(resourceFile.fileName.toString())
					if (!targetFile.exists()) {
						Files.copy(resourceFile, targetFile.toPath())
					}
				}
			}

			fs?.close()
		} catch (e: Exception) {
			logger.error("Failed to extract language files", e)
		}
	}

	private fun loadConfig() {
		try {
			logger.info(langMan.getSysMessage(FabricordMessageKey.System.LoadingConfig))

			if (Files.notExists(configFile)) {
				logger.error(langMan.getSysMessage(FabricordMessageKey.System.ConfigFileNotFound, configFile))
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
				allowMentions = config.getNullableBoolean("AllowMentions")
				webHookId = extractWebhookIdFromUrl(config.getNullableString("WebhookUrl"))
			}
		} catch (e: IOException) {
			logger.error(langMan.getSysMessage(FabricordMessageKey.System.FailedToLoadConfigByIO), e)
		} catch (e: Exception) {
			logger.error(langMan.getSysMessage(FabricordMessageKey.System.FailedToLoadConfigByUnknown), e)
		}
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