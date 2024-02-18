package com.inf_ruxy.mods.several.fabricord.util

import com.inf_ruxy.mods.several.fabricord.Fabricord.logger
import com.inf_ruxy.mods.several.fabricord.FabricordApi.dataFolder
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class ConfigManager {

    fun checkDataFolder() {
        try {
            logger.info("Checking data folder...")
            dataFolder.apply {
                when {
                    !exists() -> {
                        logger.info("Data folder does not exist. Creating data folder...")
                        mkdirs().also {
                            if (exists()) logger.info("Data folder created successfully.")
                            else logger.error("Failed to create data folder.")
                        }
                    }
                    else -> logger.info("Data folder already exists.")
                }
            }
        } catch (e: Exception) {
            logger.error("Error checking data folder.\n" + e.message)
        }
    }

    fun checkConfigFile() {
        try {
            logger.info("Checking config file...")
            val configFile = File(dataFolder, "config.yml")

            try {
                configFile.bufferedReader().use {
                    logger.info("Config file already exists.")
                }
            } catch (e: FileNotFoundException) {
                logger.info("Config file does not exist. Copying config file from Jar...")
                javaClass.getResourceAsStream("/config.yml").use { source ->
                    if (source != null) {
                        Files.copy(source, Paths.get(configFile.absolutePath), StandardCopyOption.REPLACE_EXISTING)
                        logger.info("Config file copied successfully.")
                    } else {
                        logger.error("Failed to locate the config file in the Jar.")
                    }
                }
            } catch (e: IOException) {
                logger.error("Failed to read the config file. More Details...\n${e.stackTraceToString()}")
            }

        } catch (e: Exception) {
            logger.error("Error checking config file.\n${e.stackTraceToString()}")
        }
    }


}