package com.inf_ruxy.projects.mc.plugin.several.fabricord.util

import com.inf_ruxy.projects.mc.plugin.several.fabricord.Fabricord.logger
import com.inf_ruxy.projects.mc.plugin.several.fabricord.FabricordApi.dataFolder
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.IOException


class ConfigLoader {

    var botToken: String? = null
    var logChannelID: String? = null
    var botOnlineStatus: String? = null
    var botActivityStatus: String? = null
    var botActivityMessage: String? = null

    fun loadConfig() {
        try {
            val configFile = File(dataFolder, "config.yml")
            if (!configFile.exists()) return

            val yaml = Yaml()
            try {
                FileInputStream(configFile).use { fileInputStream ->
                    val config: Map<String, Any> = yaml.load(fileInputStream)
                    botToken = config.getOrDefault("BotToken", "") as String
                    logChannelID = config.getOrDefault("Log_Channel_ID", "") as String
                    botOnlineStatus = config.getOrDefault("Bot_Online_Status", "") as String
                }
            } catch (e: IOException) {
                logger.error("Failed to load config file", e)
            }
        } catch (e: Exception) {
            logger.error("An error occurred while loading config:", e)
        }
    }

    fun isBotTokenOrLogChannelIDNull(): Boolean {
        return botToken == null || logChannelID == null
    }

}