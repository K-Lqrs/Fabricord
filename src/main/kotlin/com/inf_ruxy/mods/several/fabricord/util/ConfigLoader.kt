package com.inf_ruxy.mods.several.fabricord.util

import com.inf_ruxy.mods.several.fabricord.Fabricord.logger
import com.inf_ruxy.mods.several.fabricord.FabricordApi.dataFolder
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
            if (!configFile.exists()) {
                logger.error("Config file not found.")
                return
            }

            FileInputStream(configFile).use { fileInputStream ->
                val yaml = Yaml()
                val config: Map<String, Any> = yaml.load(fileInputStream)

                doConsoleBridge = config["Enable_Console_Bridge"] as? Boolean ?: false
                botToken = config["BotToken"] as? String
                logChannelID = config["Log_Channel_ID"] as? String
                consoleLogChannelID = config["Console_Log_Channel_ID"] as? String
                botOnlineStatus = config["Bot_Online_Status"] as? String
                botActivityStatus = config["Bot_Activity_Status"] as? String
                botActivityMessage = config["Bot_Activity_Message"] as? String
                messageStyle = config["Message_Style"] as? String
                webHookUrl = config["WebHook_URL"] as? String
            }
        } catch (e: IOException) {
            logger.error("Failed to load config file.", e)
        } catch (e: Exception) {
            logger.error("An error occurred while loading config:", e)
        }
    }


}