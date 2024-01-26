package com.inf_ruxy.several.mods.fabricord.util

import com.inf_ruxy.several.mods.fabricord.Fabricord.logger
import com.inf_ruxy.several.mods.fabricord.FabricordApi.dataFolder
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.IOException


class ConfigLoader {

    var doConsoleBridge: Boolean? = null
    var botToken: String? = null
    var logChannelID: String? = null
    var consoleLogChannelID: String? = null
    var botOnlineStatus: String? = null
    var botActivityStatus: String? = null
    var botActivityMessage: String? = null
    var messageStyle: String? = null
    var webHookUrl: String? = null

    fun loadConfig() {
        try {
            val configFile = File(dataFolder, "config.yml")
            if (!configFile.exists()) return

            val yaml = Yaml()
            try {
                FileInputStream(configFile).use { fileInputStream ->
                    val config: Map<String, Any> = yaml.load(fileInputStream)
                    doConsoleBridge = config.getOrDefault("Enable_Console_Bridge", false) as Boolean
                    botToken = config.getOrDefault("BotToken", null) as String
                    logChannelID = config.getOrDefault("Log_Channel_ID", null) as String
                    consoleLogChannelID = config.getOrDefault("Console_Log_Channel_ID", null) as String
                    botOnlineStatus = config.getOrDefault("Bot_Online_Status", null) as String
                    botActivityStatus = config.getOrDefault("Bot_Activity_Status", null) as String
                    botActivityMessage = config.getOrDefault("Bot_Activity_Message", null) as String
                    messageStyle = config.getOrDefault("Message_Style", null) as String
                    webHookUrl = config.getOrDefault("WebHook_URL", null) as String
                }
            } catch (e: IOException) {
                logger.error("Failed to load config file", e)
            }
        } catch (e: Exception) {
            logger.error("An error occurred while loading config:", e)
        }
    }

    fun isBotTokenAndLogChannelIDNull(): Boolean {
        return botToken == null || logChannelID == null
    }

    fun doBridgeConsole(): Boolean {
        return doConsoleBridge == true && consoleLogChannelID != null
    }

}