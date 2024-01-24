package com.inf_ruxy.projects.mc.plugin.several.fabricord.discord.events

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import com.inf_ruxy.projects.mc.plugin.several.fabricord.Fabricord.logger
import com.inf_ruxy.projects.mc.plugin.several.fabricord.FabricordApi.config
import com.inf_ruxy.projects.mc.plugin.several.fabricord.FabricordApi.discordBotManager
import net.minecraft.server.network.ServerPlayerEntity

class MCMessageListener {

    fun handleMCMessage(player: ServerPlayerEntity, sender: String, message: String) {
        when (config.messageStyle) {
            "modern" -> modernStyle(player, sender, message)
            "classic" -> classicStyle(sender, message)
            else -> classicStyle(sender, message)
        }
    }

    private fun modernStyle(player: ServerPlayerEntity, sender: String, message: String) {
        val playerHeadURL = "https://visage.surgeplay.com/head/256/${player.uuid}"
        WebhookClient.withUrl(config.webHookUrl!!).use { client ->
            val builder = WebhookMessageBuilder()
            builder.setUsername(sender)
            builder.setAvatarUrl(playerHeadURL)
            builder.setContent(message)

            client.send(builder.build()).exceptionally { throwable ->
                logger.error("Error sending message: ${throwable.message}")
                null
            }
        }
    }

    private fun classicStyle(mcId: String, message: String) {
        val formattedMessage = "<$mcId> » $message"
        discordBotManager.sendToDiscord(formattedMessage)
    }

}