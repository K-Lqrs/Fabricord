package net.ririfa.fabricord

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.translation.FabricordMessageProvider
import net.ririfa.langman.LangMan
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

		val logger: Logger = LoggerFactory.getLogger(Fabricord::class.simpleName)
		val loader: FabricLoader = FabricLoader.getInstance()
		val serverDir: Path = loader.gameDir
		val modDir: Path = serverDir.resolve(MOD_ID)
		val langDir: Path = modDir.resolve("lang")

		val thread: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
	}

	override fun onInitializeServer() {
		langMan = LangMan.createNew(
			{ Text.of(it) },
			FabricordMessageKey::class,
			false
		)
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
		ServerLifecycleEvents.SERVER_STARTED.register { server ->
			Fabricord.server = server
		}
	}
}