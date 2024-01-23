package com.inf_ruxy.projects.mc.plugin.several.fabricord.util

import com.inf_ruxy.projects.mc.plugin.several.fabricord.Fabricord.MOD_ID
import com.inf_ruxy.projects.mc.plugin.several.fabricord.Fabricord.logger
import net.fabricmc.loader.api.FabricLoader
import java.io.File

class ConfigManager {

    private var dataFolder: File = File(FabricLoader.getInstance().gameDir.toFile(), MOD_ID)

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


}