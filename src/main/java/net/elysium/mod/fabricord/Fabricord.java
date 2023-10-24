package net.elysium.mod.fabricord;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.elysium.mod.fabricord.discord.DiscordBot;
import net.minecraft.server.MinecraftServer;

public class Fabricord implements ModInitializer {

	private DiscordBot discordBot;

	@Override
	public void onInitialize() {
		// Register server lifecycle listeners
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
	}

	private void onServerStarting(MinecraftServer server) {
		this.discordBot = new DiscordBot(server);
		discordBot.registerEventListeners();
		discordBot.startBot();
	}

	private void onServerStopping(MinecraftServer server) {
		discordBot.stopBot();
		this.discordBot = null;
	}
}
