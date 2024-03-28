package com.inf_ruxy.fabricord

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * This class represents the main mod initializer for the Fabricord mod.
 * It implements the ModInitializer interface, which means it provides an onInitialize method that gets called when the mod is initialized.
 */
@Suppress("unused")
class Fabricord : ModInitializer {

    companion object {
        /**
         * The MOD_ID constant holds the ID of the mod, which is "fabricord".
         */
        const val MOD_ID = "fabricord"

        /**
         * The logger is used to log information, warnings, errors etc. during the mod's lifecycle.
         * It is named after the mod ("Fabricord").
         */
        val logger: Logger = LogManager.getLogger("Fabricord")
    }

    /**
     * This method is called when the mod is initialized.
     * It logs the start of the initialization, registers server lifecycle events, and logs the end of the initialization.
     */
    override fun onInitialize() {
        // Log the start of the initialization
        logger.info("    ")
        logger.info("Initializing $MOD_ID...")
        logger.info("    ")

        // Register server lifecycle events
        ServerLifecycleEvents.SERVER_STARTING.register(FabricordApi::serverStarting)
        ServerLifecycleEvents.SERVER_STARTED.register(FabricordApi::serverStarted)
        ServerLifecycleEvents.SERVER_STOPPING.register(FabricordApi::serverStopping)
    }

}