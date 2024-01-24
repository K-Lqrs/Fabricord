package com.inf_ruxy.projects.mc.plugin.several.fabricord

import com.inf_ruxy.projects.mc.plugin.several.fabricord.Fabricord.MOD_ID
import com.inf_ruxy.projects.mc.plugin.several.fabricord.Fabricord.logger
import com.inf_ruxy.projects.mc.plugin.several.fabricord.discord.DiscordBotManager
import com.inf_ruxy.projects.mc.plugin.several.fabricord.discord.DiscordEmbed
import com.inf_ruxy.projects.mc.plugin.several.fabricord.discord.events.DiscordMessageListener
import com.inf_ruxy.projects.mc.plugin.several.fabricord.util.ConfigLoader
import com.inf_ruxy.projects.mc.plugin.several.fabricord.util.ConfigManager
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import java.io.File

object FabricordApi {

    private lateinit var configManager: ConfigManager
    private lateinit var discordBotManager: DiscordBotManager
    lateinit var discordEmbed: DiscordEmbed
    lateinit var config: ConfigLoader
    lateinit var dml: DiscordMessageListener

    val dataFolder: File = File(FabricLoader.getInstance().gameDir.toFile(), MOD_ID)

    fun serverStarting(server: MinecraftServer) {
        configManager = ConfigManager()
        config = ConfigLoader()
        discordBotManager = DiscordBotManager()
        discordEmbed = DiscordEmbed()
        dml = DiscordMessageListener(server)

        configManager.checkDataFolder()
        configManager.checkConfigFile()
        config.loadConfig()
        if (config.isBotTokenOrLogChannelIDNull()) {
            logger.error("Bot token or log channel ID is null. Please check your config file.")
            return
        } else {
            discordBotManager.startBot()
            discordBotManager.registerEventListeners()
        }

    }

    fun serverStopping(server: MinecraftServer) {
        if (discordBotManager.jda == null) return
        if (config.isBotTokenOrLogChannelIDNull()) {
            discordBotManager.stopBot()
        }
    }


}