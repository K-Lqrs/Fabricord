package net.rk4z.fabricord

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModMetadata
import net.minecraft.server.MinecraftServer
import net.rk4z.beacon.*
import net.rk4z.fabricord.discord.DiscordBotManager
import net.rk4z.fabricord.discord.DiscordBotManager.botIsInitialized
import net.rk4z.fabricord.discord.DiscordMessageHandler
import net.rk4z.fabricord.discord.DiscordPlayerEventHandler
import net.rk4z.fabricord.events.*
import net.rk4z.fabricord.util.Utils.copyResourceToFile
import net.rk4z.fabricord.util.Utils.getNullableBoolean
import net.rk4z.fabricord.util.Utils.getNullableString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.bufferedWriter
import kotlin.io.path.notExists

@Suppress("unused", "MemberVisibilityCanBePrivate")
@EventHandler
object Fabricord : IEventHandler, DedicatedServerModInitializer {
    //region Constants
    const val MOD_ID = "fabricord"

    val logger: Logger = LoggerFactory.getLogger(this::class.java.simpleName)

    val loader: FabricLoader = FabricLoader.getInstance()

    private val meta: ModMetadata = loader.getModContainer(MOD_ID).get().metadata
    val name: String = meta.name
    val version: String = meta.version.friendlyString

    val serverDir: Path = loader.gameDir.toRealPath()
    val modDir: Path = serverDir.resolve(MOD_ID)
    val configFile: Path = modDir.resolve("config.yml")

    private val logContainer: MutableList<String> = mutableListOf()
    private val yaml = Yaml()
    private var initializeIsDone = false

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

    fun addLog(log: String) {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        val formattedLog = "[$timeStamp] > $log"
        logContainer.add(formattedLog)
    }

    override fun onInitializeServer() {
        EventBus.initialize()
        ServerLifecycleEvents.SERVER_STARTING.register { s: MinecraftServer ->
            EventBus.post(ServerInitEvent.get(s)) }
        ServerLifecycleEvents.SERVER_STARTED.register { EventBus.post(ServerStartEvent.get()) }
        ServerLifecycleEvents.SERVER_STOPPING.register { EventBus.post(ServerShutdownEvent.get()) }
    }

    val onServerInit = handler<ServerInitEvent>(
        condition = { true },
        priority = Priority.HIGHEST
    ) { e ->
        try {
            addLog("Server initializing...")
            logger.info("Initializing $name v$version")
            DiscordBotManager.init(e.s)
            DiscordMessageHandler()
            DiscordPlayerEventHandler()
            checkRequiredFilesAndDirectories()
            loadConfig()
            nullCheck()
            initializeIsDone = true
        } catch (e: Exception) {
            addLog("An unexpected error occurred while initializing the server")
            logger.error("An unexpected error occurred while initializing the server", e)
        }
    }

    val onServerStart = handler<ServerStartEvent>(
        condition = { true },
        priority = Priority.HIGHEST
    ) { e ->
        addLog("Server starting...")
        logger.info("Starting $name v$version")
        if (requiredNullCheck()) {
            addLog("Bot token or log channel ID is missing in config file.")
            logger.warn("Bot token or log channel ID is missing in config file.")
            logger.warn("Maybe you are running the mod for the first time?")
            logger.warn("Please check the config file at $configFile")
            return@handler
        } else {
            DiscordBotManager.startBot()
        }
    }

    val onServerStop = handler<ServerShutdownEvent>(
        condition = { true },
        priority = Priority.HIGHEST
    ) {
        addLog("Server stopping...")
        logger.info("Stopping $name v$version")
        if (!botIsInitialized) {
            logger.warn("Discord bot is not initialized. Cannot shutdown bot.")
        } else {
            DiscordBotManager.stopBot()
        }
        logDump()
        EventBus.shutdown()
    }

    private fun logDump() {
        try {
            addLog("Dumping logs to file...")
            logger.info("Dumping logs to file. Please wait...")
            val timeStamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Date())
            val logFile = modDir.resolve("log_${name}_$timeStamp.txt")
            logFile.bufferedWriter().use { writer ->
                logContainer.forEach { log ->
                    writer.write(log)
                    writer.newLine()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun checkRequiredFilesAndDirectories() {
        try {
            if (!Files.exists(modDir)) {
                addLog("Creating config directory at $modDir")
                logger.info("Creating config directory at $modDir")
                Files.createDirectories(modDir)
            }
            addLog("Checking config file at $configFile")
            if (configFile.notExists()) {
                copyResourceToFile("config.yml", configFile)
            }
        } catch (e: SecurityException) {
            addLog("Failed to create/check required files or directories due to security restrictions")
            logger.error("Failed to create/check required files or directories due to security restrictions", e)
        } catch (e: IOException) {
            addLog("Failed to create/check required files or directories due to an I/O error")
            logger.error("Failed to create/check required files or directories due to an I/O error", e)
        } catch (e: Exception) {
            addLog("An unexpected error occurred while creating/checking required files or directories")
            logger.error("An unexpected error occurred while creating/checking required files or directories", e)
        }
    }

    private fun loadConfig() {
        try {
            addLog("Loading config file...")
            logger.info("Loading config file...")

            if (Files.notExists(configFile)) {
                addLog("Config file not found at $configFile")
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
            addLog("Failed to load config file")
            logger.error("Failed to load config file", e)
        } catch (e: Exception) {
            addLog("An unexpected error occurred while loading config")
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