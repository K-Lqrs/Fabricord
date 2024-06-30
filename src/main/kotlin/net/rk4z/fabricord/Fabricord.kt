package net.rk4z.fabricord

import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModMetadata
import net.rk4z.beacon.Listener
import net.rk4z.beacon.Priority
import net.rk4z.beacon.handler
import net.rk4z.fabricord.events.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*

@Suppress("unused", "MemberVisibilityCanBePrivate")
object Fabricord : Listener {
    const val MOD_ID = "fabricord"

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    val loader: FabricLoader = FabricLoader.getInstance()

    private val meta: ModMetadata = loader.getModContainer(MOD_ID).get().metadata
    val name: String = meta.name
    val version: String = meta.version.friendlyString

    val serverDir: Path = loader.gameDir.toAbsolutePath()
    val modDir: Path = serverDir.resolve(MOD_ID)
    val configFile: Path = modDir.resolve("config.yml")

    private val logContainer: MutableList<String> = mutableListOf()
    fun addLog(log: String) {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        val formattedLog = "[$timeStamp] > $log"
        logContainer.add(formattedLog)
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

    val onServerStart = handler<ServerStartEvent>(
        priority = Priority.HIGHEST
    ) {
        addLog("Server starting...")
        logger.info("Starting $name v$version")
    }

    val onServerStop = handler<ServerStopEvent>(
        priority = Priority.HIGHEST
    ) {
        addLog("Server stopping...")
        logger.info("Stopping $name v$version")
        logDump()
    }

}