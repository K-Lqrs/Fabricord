package net.rk4z.fabricord.discord

import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.minecraft.server.network.ServerPlayerEntity
import net.rk4z.fabricord.Fabricord

object DiscordPlayerEventHandler {
    fun handleMCMessage(player: ServerPlayerEntity, message: String) {
        Fabricord.executorService.submit {
            when (Fabricord.messageStyle) {
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
        if (Fabricord.webHookId.isNullOrBlank()) {
            Fabricord.logger.error("Webhook URL is not configured or blank.")
            return
        }

        try {
            val webHookClient = DiscordBotManager.webHook

            val data = MessageCreateBuilder()
                .setContent(message)
                .build()

            webHookClient!!.sendMessage(data)
                .setUsername(player.name.string)
                .setAvatarUrl("https://visage.surgeplay.com/face/256/${player.uuid}")
                .queue()

        } catch (e: Exception) {
            Fabricord.logger.error("An unexpected error occurred while sending message to Discord webhook: ${e.localizedMessage}", e)
        }
    }
}
