package net.elysium.mod.fabricord;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.elysium.mod.fabricord.discord.DiscordBot;
import net.minecraft.server.MinecraftServer;

public class Fabricord implements ModInitializer, ServerLifecycleEvents.ServerStarted, ServerLifecycleEvents.ServerStopped {

	private DiscordBot discordBot;  // Change to non-final
	private MinecraftServer server;

	@Override
	public void onInitialize() {
		// Register server lifecycle listeners
		ServerLifecycleEvents.SERVER_STARTED.register(this);
		ServerLifecycleEvents.SERVER_STOPPED.register(this);
	}

	@Override
	public void onServerStarted(MinecraftServer server) {
		this.server = server;
		this.discordBot = new DiscordBot(server);  // Create DiscordBot instance here with server
		discordBot.registerEventListeners();
		discordBot.startBot();
	}

	@Override
	public void onServerStopped(MinecraftServer server) {
		discordBot.stopBot();
		this.server = null;
		this.discordBot = null;  // Set discordBot to null
	}
}
