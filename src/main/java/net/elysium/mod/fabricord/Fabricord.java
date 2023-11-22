package net.elysium.mod.fabricord;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.elysium.mod.fabricord.discord.DiscordBot;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Fabricord implements ModInitializer {

	public static final String MOD_ID = "fabricord";
	static final Logger LOGGER = LogManager.getLogger(Fabricord.class);

	private DiscordBot discordBot;
	private static final ConfigManager configManager = new ConfigManager();

	public Fabricord() {
	}

	@Override
	public void onInitialize() {
		try {
			// Register server lifecycle listeners
			ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
			ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
		} catch (Exception e) {
			LOGGER.error("Error during initialization: ", e);
		}
	}

	private void onServerStarting(MinecraftServer server) {
		try {
			this.discordBot = new DiscordBot(server);
			configManager.checkAndCreateConfig();
			discordBot.registerEventListeners();
			discordBot.startBot();
		} catch (Exception e) {
			LOGGER.error("Error during server starting: ", e);
		}
	}

	private void onServerStopping(MinecraftServer server) {
		try {
			discordBot.stopBot();
			this.discordBot = null;
		} catch (Exception e) {
			LOGGER.error("Error during server stopping: ", e);
		}
	}
}
