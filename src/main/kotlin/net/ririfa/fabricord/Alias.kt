package net.ririfa.fabricord

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger
import java.nio.file.Path

val FC = Fabricord.instance
val LM = Fabricord.langMan
val Logger: Logger = Fabricord.logger
val Server: MinecraftServer = Fabricord.server
val Loader: FabricLoader = Fabricord.loader
val ServerDir: Path = Fabricord.serverDir
val ModDir: Path = Fabricord.modDir
val ConfigDir: Path = ConfigManager.configFile