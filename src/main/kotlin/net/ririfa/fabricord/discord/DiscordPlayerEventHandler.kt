package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.minecraft.server.network.ServerPlayerEntity
import net.ririfa.fabricord.Config
import net.ririfa.fabricord.FT
import net.ririfa.fabricord.LM
import net.ririfa.fabricord.Logger

object DiscordPlayerEventHandler {
	fun handleMCMessage(player: ServerPlayerEntity, message: String) {
		FT {
			when (Config.messageStyle) {
				"modern" -> modernStyle(player, message)
				"classic" -> classicStyle(player, message)
				else -> classicStyle(player, message)
			}
		}
	}

	private fun classicStyle(player: ServerPlayerEntity, message: String) {
		val mcId = player.name.string
		val formattedMessage = "$mcId » $message"
		DiscordBotManager.sendToDiscord(formattedMessage)
	}

	private fun modernStyle(player: ServerPlayerEntity, message: String) {
		if (Config.webHookId.isNullOrBlank()) {
			Logger.error(LM.getSysMessage(TODO()))
		}

		try {
			val webHookClient = DiscordBotManager.webHook ?: run {
				Logger.error(LM.getSysMessage(TODO()))
				return
			}

			val data = MessageCreateBuilder()
				.setContent(message)

			if (Config.allowMentions == false) {
				data.setAllowedMentions(emptySet())
			}

			webHookClient.sendMessage(data.build())
				.setUsername(player.name.string)
				.setAvatarUrl("https://visage.surgeplay.com/face/256/${player.uuid}")
				.queue()

		} catch (e: Exception) {
			Logger.error(LM.getSysMessage(TODO()), e)
		}
	}
}