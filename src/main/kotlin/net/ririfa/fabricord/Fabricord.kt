package net.ririfa.fabricord

import com.mojang.brigadier.CommandDispatcher
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
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.ririfa.fabricord.discord.DiscordBotManager
import net.ririfa.fabricord.discord.DiscordEmbed
import net.ririfa.fabricord.discord.DiscordPlayerEventHandler.handleMCMessage
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.translation.FabricordMessageProvider
import net.ririfa.fabricord.translation.adapt
import net.ririfa.langman.InitType
import net.ririfa.langman.LangMan
import org.apache.logging.log4j.LogManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

class Fabricord : DedicatedServerModInitializer {
	companion object {
		const val MOD_ID = "fabricord"
		val instance: Fabricord by lazy { Fabricord() }

		lateinit var server: MinecraftServer
		lateinit var langMan: LangMan<FabricordMessageProvider, Text>
		lateinit var consoleAppender: ConsoleTrackerAppender

		val logger: Logger = LoggerFactory.getLogger(Fabricord::class.simpleName)
		val loader: FabricLoader = FabricLoader.getInstance()
		val serverDir: Path = loader.gameDir
		val modDir: Path = serverDir.resolve(MOD_ID)
		val langDir: Path = modDir.resolve("lang")

		val thread: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
		val availableLang = listOf<String>("en", "ja")

		private val localChatToggled = mutableListOf<UUID>()
	}

	override fun onInitializeServer() {
		extractLangFiles()
		langMan = LangMan.createNew(
			{ Text.of(it) },
			FabricordMessageKey::class,
			false
		)
		langMan.init(
			InitType.YAML,
			langDir.toFile(),
			availableLang
		)
		availableLang.forEach {
			langMan.logMissingKeys(it)
		}
		ConfigManager.init()
		registerServerEvents()
	}

	private fun extractLangFiles() {
		try {
			val targetDir = Paths.get("$ModDir/lang")
			if (!Files.exists(targetDir)) {
				Files.createDirectories(targetDir)
			}

			val devSourceDir = Paths.get("build/resources/main/assets/$MOD_ID/lang")
			if (Files.exists(devSourceDir)) {
				copyLanguageFiles(devSourceDir, targetDir)
				return
			}

			val langPath = "assets/$MOD_ID/lang/"
			val classLoader = this::class.java.classLoader
			val resourceUrl = classLoader.getResource(langPath)

			if (resourceUrl == null) {
				logger.error("Failed to find language directory in JAR: $langPath")
				return
			}

			val uri = resourceUrl.toURI()
			val fs = if (uri.scheme == "jar") FileSystems.newFileSystem(uri, emptyMap<String, Any>()) else null
			val langDirPath = Paths.get(uri)

			copyLanguageFiles(langDirPath, targetDir)

			fs?.close()
		} catch (e: Exception) {
			logger.error("Failed to extract language files", e)
		}
	}

	private fun copyLanguageFiles(sourceDir: Path, targetDir: Path) {
		Files.walk(sourceDir).use { paths ->
			paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".yml") }.forEach { resourceFile ->
				val targetFile = targetDir.resolve(resourceFile.fileName.toString())
				if (!Files.exists(targetFile)) {
					Files.copy(resourceFile, targetFile)
				}
			}
		}
	}

	private fun registerServerEvents() {
		consoleAppender = ConsoleTrackerAppender("FabricordConsoleTracker")
		val rootLogger = LogManager.getRootLogger() as org.apache.logging.log4j.core.Logger
		rootLogger.addAppender(consoleAppender)

		CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
			//			GroupManager.registerAll(dispatcher)
			registerLCCommand(dispatcher)
		}
		ServerLifecycleEvents.SERVER_STARTED.register { server ->
			Fabricord.server = server
			if (!(ConfigManager.isErrorOccurred)) {
				DiscordBotManager.start()
			}
		}
		ServerLifecycleEvents.SERVER_STOPPING.register { server ->
			if (DiscordBotManager.botIsInitialized) {
				DiscordBotManager.stop()
			}
			rootLogger.removeAppender(consoleAppender)
			consoleAppender.stop()
		}

		ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
			val player = handler.player

			if (DiscordBotManager.botIsInitialized) {
				FT {
					DiscordEmbed.sendPlayerJoinEmbed(player)
				}
			}
		}
		ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, _ ->
			if (DiscordBotManager.botIsInitialized) {
				FT {
					val player = handler.player
					DiscordEmbed.sendPlayerLeftEmbed(player)
				}
			}
		})
		ServerMessageEvents.CHAT_MESSAGE.register(ServerMessageEvents.ChatMessage { message, sender, params ->
			if (DiscordBotManager.botIsInitialized) {
				val uuid = sender.uuid

				//TODO: Also consider group chats -> [GroupManager]
				// But probably controllable via Mixin.
				if (uuid in localChatToggled) return@ChatMessage

				val content = message.content.string
				handleMCMessage(sender, content)
			}
		})
	}

	private fun registerLCCommand(dispatcher: CommandDispatcher<ServerCommandSource>) {
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
					val stateMSG = if (newState == true) {
						"ON"
					} else {
						"OFF"
					}
					player.sendMessage(
						player.adapt().getMessage(FabricordMessageKey.Command.LC.SwitchedLocalChatState, stateMSG),
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
								it.sendMessage(
									Text.of(
										"<${player.name.string}> $message"
									),
									false
								)
							}

							return@executes 1
						}
				)
		)

	}
}