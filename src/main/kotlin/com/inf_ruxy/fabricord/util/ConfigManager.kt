package com.inf_ruxy.fabricord.util

import com.inf_ruxy.fabricord.Fabricord.Companion.MOD_ID
import com.inf_ruxy.fabricord.Fabricord.Companion.logger
import net.fabricmc.loader.api.FabricLoader
import org.yaml.snakeyaml.Yaml
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * This class manages the configuration for the application.
 * It provides methods for checking and loading the configuration from a file.
 * The configuration properties are accessed through the companion object.
 */
class ConfigManager {

    /**
     * The [Companion] class contains static properties for managing the configuration of the application.
     *
     * @property dataFolder The data folder for the application.
     * @property configFile The configuration file for the application.
     * @property jarConfigFile The configuration file packaged in the JAR.
     * @property botToken The token for the bot.
     * @property logChannelID The ID of the log channel.
     * @property botOnlineStatus The online status of the bot.
     * @property botActivityStatus The activity status of the bot.
     * @property botActivityMessage The activity message of the bot.
     * @property messageStyle The style of the messages.
     * @property webHookUrl The webhook URL.
     */
    companion object {
        val dataFolder: Path = Paths.get(FabricLoader.getInstance().gameDir.toString(), MOD_ID)
        val configFile: Path = dataFolder.resolve("config.yml")
        val jarConfigFile: Path? =
            ConfigManager::class.java.classLoader.getResource("config.yml")?.toURI()?.let { Paths.get(it) }


        var botToken: String? = null
        var logChannelID: String? = null
        var botOnlineStatus: String? = null
        var botActivityStatus: String? = null
        var botActivityMessage: String? = null
        var messageStyle: String? = null
        var webHookUrl: String? = null
    }

    /**
     * Checks the configuration by performing two checks: checking the data folder and checking the config file.
     */
    fun configCheck() {
        val configChecker = ConfigChecker()
        configChecker.checkDataFolder()
        configChecker.checkConfigFile()
    }

    /**
     * Loads the configuration file for the ConfigManager.
     * It uses the ConfigLoader class to perform the loading and null checks.
     */
    fun configLoad() {
        val configLoader = ConfigLoader()
        configLoader.loadConfig()
        configLoader.nullCheck()
    }

    /**
     * The ConfigChecker class is responsible for checking the data folder and config file.
     */
    private class ConfigChecker {

        /**
         * Checks the data folder and creates it if it does not exist.
         * Logs the progress and any errors that occur.
         */
        fun checkDataFolder() {
            try {
                logger.info("Checking data folder...")
                if (Files.notExists(dataFolder)) {
                    logger.info("Data folder does not exist. Creating data folder...")
                    Files.createDirectories(dataFolder)
                    logger.info("Data folder created successfully.")
                } else {
                    logger.info("Data folder already exists.")
                }
            } catch (e: SecurityException) {
                logger.error("Failed to create/check data folder due to security restrictions.", e)
                logger.error(e.stackTraceToString())
            } catch (e: IOException) {
                logger.error("Failed to create/check data folder due to an IO error.", e)
                logger.error(e.stackTraceToString())
            } catch (e: Exception) {
                logger.error("An unexpected error occurred while checking/creating the data folder.", e)
                logger.error(e.stackTraceToString())
            }
        }


        /**
         * Checks the configuration file.
         * If the file does not exist, it creates the config file from the JAR file.
         * Logs the progress and any errors that occur.
         */
        fun checkConfigFile() {
            try {
                logger.info("Checking config file...")
                if (Files.notExists(configFile)) {
                    logger.info("Config file does not exist. Creating config file...")
                    try {
                        jarConfigFile?.let { jarPath ->
                            Files.copy(jarPath, configFile, StandardCopyOption.REPLACE_EXISTING)
                            logger.info("Config file was successfully created from JAR.")
                        } ?: throw IOException("Config file stream could not be opened.")
                    } catch (e: SecurityException) {
                        logger.error("Failed to create config file due to security restrictions.", e)
                        logger.error(e.stackTraceToString())
                    } catch (e: IOException) {
                        logger.error("Failed to create config file due to an IO error.", e)
                        logger.error(e.stackTraceToString())
                    }
                } else {
                    logger.info("Config file already exists.")
                }
            } catch (e: SecurityException) {
                logger.error("Error checking config file due to security restrictions.", e)
                logger.error(e.stackTraceToString())
            } catch (e: IOException) {
                logger.error("Error checking config file due to an IO error.", e)
                logger.error(e.stackTraceToString())
            } catch (e: Exception) {
                logger.error("Unexpected error checking config file.", e)
                logger.error(e.stackTraceToString())
            }
        }

    }

    /**
     * The ConfigLoader class is responsible for loading the configuration from a YAML file.
     * It also performs null checks for the loaded values.
     */
    private class ConfigLoader {

        /**
         * Loads the configuration from the "config.yml" file.
         * If the file doesn't exist, an error message is logged and the method returns.
         * The method reads the file, parses its contents with YAML, and assigns values to different variables.
         * The variables correspond to different configuration settings retrieved from the YAML file.
         * If any I/O or parsing error occurs, it is logged with a stack trace.
         */
        fun loadConfig() {
            try {
                if (Files.notExists(configFile)) {
                    logger.error("Config file not found. Please restart the server to generate a new config file.")
                    return
                }

                Files.newInputStream(configFile).use { fileInputStream ->
                    val yaml = Yaml()
                    val config: Map<String, Any> = yaml.load(fileInputStream)

                    botToken = config.getNullableString("BotToken")
                    logChannelID = config.getNullableString("Log_Channel_ID")
                    botOnlineStatus = config.getNullableString("Bot_Online_Status")
                    botActivityStatus = config.getNullableString("Bot_Activity_Status")
                    botActivityMessage = config.getNullableString("Bot_Activity_Message")
                    messageStyle = config.getNullableString("Message_Style")
                    webHookUrl = config.getNullableString("WebHook_URL")
                }

                logger.info("Config loaded successfully.")
            } catch (e: IOException) {
                logger.error("Failed to load config file.", e)
                logger.error(e.stackTraceToString())
            } catch (e: Exception) {
                logger.error("An unexpected error occurred while loading config:", e)
                logger.error(e.stackTraceToString())
            }
        }

        /**
         * Retrieves a nullable string value from the map associated with the given key.
         * Returns null if the value is null, or if the value is a blank string.
         *
         * @param key The key used to retrieve the value from the map.
         * @return The nullable string value associated with the given key, or null if the value is null or blank.
         */
        private fun Map<String, Any>.getNullableString(key: String): String? =
            this[key]?.toString()?.takeIf { it.isNotBlank() }


        /**
         * Performs null checks on several variables and assigns default values if they are null.
         */
        fun nullCheck() {

            if (botActivityMessage.isNullOrBlank()) {
                botActivityMessage = "Minecraft Server"
            }

            if (messageStyle.isNullOrBlank()) {
                messageStyle = "classic"
            }

            if (botActivityStatus.isNullOrBlank()) {
                botActivityStatus = "playing"
            }

        }

    }

    fun requiredNullCheck(): Boolean {
        return botToken.isNullOrBlank() || logChannelID.isNullOrBlank()
    }

}