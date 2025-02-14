package net.ririfa.fabricord

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.ririfa.fabricord.discord.DiscordBotManager
import net.ririfa.fabricord.discord.DiscordEmbed
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
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import kotlin.io.path.exists

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
		ConfigManager.init()
		registerServerEvents()
	}

	private fun extractLangFiles() {
		try {
			val langPath = "assets/fabricord/lang/"
			val classLoader = this::class.java.classLoader
			val resourceUrl = classLoader.getResource(langPath)

			if (resourceUrl == null) {
				logger.error("Failed to find language directory in JAR: $langPath")
				return
			}

			val uri = resourceUrl.toURI()
			val fs = if (uri.scheme == "jar") FileSystems.newFileSystem(uri, emptyMap<String, Any>()) else null
			val langDirPath = Paths.get(uri)

			Files.walk(langDirPath).use { paths ->
				paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".yml") }.forEach { resourceFile ->
					val targetFile = langDir.resolve(resourceFile.fileName.toString())
					if (!targetFile.exists()) {
						Files.copy(resourceFile, targetFile)
					}
				}
			}

			fs?.close()
		} catch (e: Exception) {
			logger.error("Failed to extract language files", e)
		}
	}

	private fun registerServerEvents() {
		consoleAppender = ConsoleTrackerAppender("FabricordConsoleTracker")
		val rootLogger = LogManager.getRootLogger() as org.apache.logging.log4j.core.Logger
		rootLogger.addAppender(consoleAppender)

		ServerLifecycleEvents.SERVER_STARTED.register { server ->
			Fabricord.server = server
			DiscordBotManager.start()
		}
		ServerLifecycleEvents.SERVER_STOPPING.register { server ->
			DiscordBotManager.stop()
			rootLogger.removeAppender(consoleAppender)
			consoleAppender.stop()
		}

		ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
			val player = handler.player

			if (!DiscordBotManager.botIsInitialized) {
				player.networkHandler.disconnect(
					player.adapt().getMessage(TODO())
				)
				return@register
			}

			FT {
				DiscordEmbed.sendPlayerJoinEmbed(player)
			}
		}
		ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, _ ->
			FT {
				val player = handler.player
				DiscordEmbed.sendPlayerLeftEmbed(player)
			}
		})
		ServerMessageEvents.CHAT_MESSAGE.register(ServerMessageEvents.ChatMessage { message, sender, params ->
			TODO()
		})
	}
}