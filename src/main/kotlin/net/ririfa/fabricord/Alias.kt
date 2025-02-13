package net.ririfa.fabricord

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.ririfa.fabricord.discord.DiscordBotManager
import org.slf4j.Logger
import java.nio.file.Path

val JDA = DiscordBotManager.jda
	get() {
		if (field == null) {
			Logger.error(LM.getSysMessage(TODO()))
		}
		return field
	}
val FC = Fabricord.instance
val LM = Fabricord.langMan
val Logger: Logger = Fabricord.logger
val Server: MinecraftServer = Fabricord.server
val Loader: FabricLoader = Fabricord.loader
val ServerDir: Path = Fabricord.serverDir
val ModDir: Path = Fabricord.modDir
val ConfigDir: Path = ConfigManager.configFile
val Config = ConfigManager.config