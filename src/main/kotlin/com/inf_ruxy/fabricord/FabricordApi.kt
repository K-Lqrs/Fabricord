package com.inf_ruxy.fabricord

import com.inf_ruxy.fabricord.Fabricord.Companion.logger
import com.inf_ruxy.fabricord.discord.DiscordBotManager
import com.inf_ruxy.fabricord.discord.DiscordEmbed
import com.inf_ruxy.fabricord.discord.events.DiscordMessageListener
import com.inf_ruxy.fabricord.discord.events.MCMessageListener
import com.inf_ruxy.fabricord.util.ConfigManager
import net.minecraft.server.MinecraftServer
import java.io.IOException

object FabricordApi {

    lateinit var config: ConfigManager
    lateinit var discordBotManager: DiscordBotManager
    lateinit var discordEmbed: DiscordEmbed
    lateinit var mcMessageListener: MCMessageListener
    lateinit var discordMessageListener: DiscordMessageListener

    fun serverStarting(server: MinecraftServer) {
        logger.info("Starting Fabricord...")
        try {
            config = ConfigManager()
            discordBotManager = DiscordBotManager()
            discordEmbed = DiscordEmbed()
            mcMessageListener = MCMessageListener()
            discordMessageListener = DiscordMessageListener(server)

            config.configCheck()
            config.configLoad()


        } catch (e: IOException) {
            logger.error("Error loading configuration for Fabricord.\n${e.message}", e)
            logger.error(e.stackTraceToString())
        } catch (e: NullPointerException) {
            logger.error("Missing required configuration for Fabricord.\n${e.message}", e)
            logger.error(e.stackTraceToString())
        } catch (e: Exception) {
            logger.error("Unexpected error initializing Fabricord.\n${e.message}", e)
            logger.error(e.stackTraceToString())
        }
    }


    fun serverStarted(server: MinecraftServer) {
        logger.info("Fabricord started.")
        if (!config.requiredNullCheck()) {
            discordBotManager.startBot()
            discordBotManager.registerEventListeners()
        } else {
            logger.warn("Fabricord General chat bridge system is disabled.")
        }
    }

    fun serverStopping(server: MinecraftServer) {
        if (!config.requiredNullCheck()) {
            logger.info("Stopping Fabricord...")
            discordBotManager.stopBot()
        }
    }

}