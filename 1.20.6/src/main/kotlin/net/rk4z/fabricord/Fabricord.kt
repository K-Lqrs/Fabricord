package net.rk4z.fabricord

import com.ibm.icu.text.SimpleDateFormat
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModMetadata
import net.rk4z.beacon.EventBus
import net.rk4z.beacon.Listener
import net.rk4z.beacon.handler
import net.rk4z.fabricord.discord.DiscordBotManager
import net.rk4z.fabricord.events.ServerStartEvent
import net.rk4z.fabricord.events.ServerStopEvent
import net.rk4z.fabricord.util.Utils.copyResourceToFile
import net.rk4z.fabricord.util.Utils.getNullableString
import net.rk4z.fabricord.util.Utils.getNullableBoolean
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Date

@Suppress("unused", "MemberVisibilityCanBePrivate")
object Fabricord : Listener {
    const val MOD_ID = "fabricord"
    val logger: Logger = LoggerFactory.getLogger(this::class.java.simpleName)

    val loader: FabricLoader = FabricLoader.getInstance()

    private val meta: ModMetadata = loader.getModContainer(MOD_ID).get().metadata
    val name: String = meta.name
    val version: String = meta.version.friendlyString

    val serverDir: Path = loader.gameDir.toAbsolutePath()
    val modDir: Path = serverDir.resolve(MOD_ID)
    val configFile: Path = modDir.resolve("config.yml")

    private val yaml = Yaml()
    val logContainer: MutableList<String> = mutableListOf()

    fun addLog(log: String) {
        logContainer.add("$log\n")
    }

    init {
        initialize()
    }

    private fun initialize() {
        EventBus.registerAllListeners("net.rk4z.fabricord")
        logger.info("Loading $name v$version")
        if (checkRequiredFilesAndDirectories()) {
            if (!requiredNullCheck()) {
                loadConfig()
                nullCheck()
            } else {
                logger.error("BotToken or LogChannelID is missing from config.yml")
                throw IllegalStateException("BotToken or LogChannelID is missing from config.yml")
            }
        }
    }

    val startHandler = handler<ServerStartEvent> {
        DiscordBotManager.startBot()
    }

    val stopHandler = handler<ServerStopEvent> {
        logger.info("Shutting down $name v$version")
        logDump()
        DiscordBotManager.stopBot()
        EventBus.shutdown()
    }

    private fun logDump() {
        try {
            val timeStamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Date())
            val logFile = File("log_${name}_$timeStamp.log")
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

    private fun checkRequiredFilesAndDirectories(): Boolean {
        try {
            if (!Files.exists(modDir)) {
                logger.info("Creating config directory at $modDir")
                Files.createDirectories(modDir)
            }
            copyResourceToFile("config.yml", configFile)
            return true
        } catch (e: SecurityException) {
            logger.error("Failed to create/check required files or directories due to security restrictions", e)
            return false
        } catch (e: IOException) {
            logger.error("Failed to create/check required files or directories due to an I/O error", e)
            return false
        } catch (e: Exception) {
            logger.error("An unexpected error occurred while creating/checking required files or directories", e)
            return false
        }
    }

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

    private fun loadConfig() {
        try {
            if (Files.notExists(configFile)) {
                logger.error("Config file not found at $configFile")
                return
            }

            Files.newInputStream(configFile).use { inputStream ->
                val config: Map<String, Any> = yaml.load(inputStream)

                // Required
                botToken = config.getNullableString("BotToken")
                logChannelID = config.getNullableString("LogChannelID")
                // Optional
                enableConsoleLog = config.getNullableBoolean("EnableConsoleLog")
                consoleLogChannelID = config.getNullableString("ConsoleLogChannelID")

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
            serverStartMessage = ":white_check_mark: Server has started!"
        }
        if (serverStopMessage.isNullOrBlank()) {
            serverStopMessage = ":octagonal_sign: Server has stopped!"
        }
    }

    private fun requiredNullCheck(): Boolean {
        return botToken.isNullOrBlank() || logChannelID.isNullOrBlank()
    }
}