package com.inf_ruxy.fabricord.discord.events

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import com.inf_ruxy.fabricord.Fabricord.Companion.logger
import com.inf_ruxy.fabricord.FabricordApi.discordBotManager
import com.inf_ruxy.fabricord.util.ConfigManager.Companion.messageStyle
import com.inf_ruxy.fabricord.util.ConfigManager.Companion.webHookUrl
import net.minecraft.server.network.ServerPlayerEntity

class MCMessageListener {

    fun handleMCMessage(player: ServerPlayerEntity, sender: String, message: String) {
        when (messageStyle) {
            "modern" -> modernStyle(player, sender, message)
            "classic" -> classicStyle(sender, message)
            else -> classicStyle(sender, message)
        }
    }

    private fun modernStyle(player: ServerPlayerEntity, sender: String, message: String) {
        if (webHookUrl.isNullOrBlank()) {
            logger.error("Webhook URL is not configured or blank.")
            return
        }

        try {
            WebhookClient.withUrl(webHookUrl!!).use { client ->
                val builder = WebhookMessageBuilder().apply {
                    setUsername(sender)
                    setAvatarUrl("https://visage.surgeplay.com/head/256/${player.uuid}")
                    setContent(message)
                }
                client.send(builder.build()).whenComplete { _, error ->
                    error?.let {
                        logger.error("Error sending message to Discord webhook: ${error.localizedMessage}", error)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("An unexpected error occurred while sending message to Discord webhook: ${e.localizedMessage}", e)
        }
    }


    private fun classicStyle(mcId: String, message: String) {
        val formattedMessage = "$mcId » $message"
        discordBotManager.sendToDiscord(formattedMessage)
    }

}