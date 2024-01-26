package com.inf_ruxy.several.mods.fabricord.util

import com.inf_ruxy.several.mods.fabricord.Fabricord.logger
import com.inf_ruxy.several.mods.fabricord.FabricordApi.dataFolder
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

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
            configFile.apply {
                when {
                    !exists() -> {
                        logger.info("Config file does not exist. Copying config file from Jar...")
                        javaClass.getResourceAsStream("/config.yml").use { source ->
                            source?.let { Files.copy(it, Paths.get(absolutePath)) }
                        }
                        if (exists()) logger.info("Config file copied successfully.")
                        else logger.error("Failed to copy config file.")
                    }
                    else -> logger.info("Config file already exists.")
                }
            }
        } catch (e: Exception) {
            logger.error("Error checking config file.\n" + e.message)
        }
    }

}