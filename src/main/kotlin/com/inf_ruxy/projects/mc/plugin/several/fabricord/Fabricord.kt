package com.inf_ruxy.projects.mc.plugin.several.fabricord

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Suppress("unused")
object Fabricord : ModInitializer {
	const val MOD_ID = "fabricord"
	val logger: Logger = LoggerFactory.getLogger("Fabricord")

	override fun onInitialize() {
		logger.info("Initializing$MOD_ID...")
		try {
			ServerLifecycleEvents.SERVER_STARTED.register(FabricordApi::serverStarted)
			ServerLifecycleEvents.SERVER_STARTING.register(FabricordApi::serverStarting)
			ServerLifecycleEvents.SERVER_STOPPING.register(FabricordApi::serverStopping)
		} catch (e: Exception) {
			logger.error("Error initializing Fabricord.\n" + e.message)
		}
	}

}