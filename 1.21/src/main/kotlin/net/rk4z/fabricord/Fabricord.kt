package net.rk4z.fabricord

import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModMetadata
import net.rk4z.beacon.EventBus
import net.rk4z.beacon.Listener
import net.rk4z.beacon.handler
import net.rk4z.fabricord.events.ServerStartEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Suppress("unused")
object Fabricord : Listener {
    const val MOD_ID = "fabricord"
    val logger: Logger = LoggerFactory.getLogger(this::class.java.simpleName)

    val loader: FabricLoader = FabricLoader.getInstance()

    private val meta: ModMetadata = loader.getModContainer(MOD_ID).get().metadata
    val name: String = meta.name
    val version: String = meta.version.friendlyString

    val serverDir = loader.gameDir.toAbsolutePath()
    val configDir = serverDir.resolve(MOD_ID)

    init {
        EventBus.registerAllListeners("net.rk4z.fabricord")
    }

    val startServer = handler<ServerStartEvent> {
        logger.info("Loading $name v$version")
    }

    private fun checkRequiredFileAndDirectories() {

    }
}