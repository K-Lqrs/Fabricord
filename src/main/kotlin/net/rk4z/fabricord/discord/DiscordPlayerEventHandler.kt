package net.rk4z.fabricord.discord

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.send.WebhookMessageBuilder
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
        if (Fabricord.webHookUrl.isNullOrBlank()) {
            Fabricord.logger.error("Webhook URL is not configured or blank.")
            return
        }

        try {
            WebhookClient.withUrl(Fabricord.webHookUrl!!).use { client ->
                val builder = WebhookMessageBuilder().apply {
                    setUsername(player.name.string)
                    setAvatarUrl("https://visage.surgeplay.com/head/256/${player.uuid}")
                    setContent(message)
                }
                client.send(builder.build()).whenComplete { _, error ->
                    handleError(error)
                }
            }
        } catch (e: Exception) {
            Fabricord.logger.error("An unexpected error occurred while sending message to Discord webhook: ${e.localizedMessage}", e)
        }
    }

    private fun handleError(error: Throwable?) {
        error?.let {
            Fabricord.logger.error("Error sending message to Discord webhook: ${it.localizedMessage}", it)
        }
    }
}
