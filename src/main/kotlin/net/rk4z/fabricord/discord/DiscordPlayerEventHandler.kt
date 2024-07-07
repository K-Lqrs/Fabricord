package net.rk4z.fabricord.discord

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.network.listener.ServerPlayPacketListener
import net.minecraft.network.message.MessageType
import net.minecraft.network.message.SignedMessage
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.rk4z.beacon.EventBus
import net.rk4z.beacon.EventHandler
import net.rk4z.beacon.IEventHandler
import net.rk4z.beacon.handler
import net.rk4z.fabricord.Fabricord.logger
import net.rk4z.fabricord.Fabricord.messageStyle
import net.rk4z.fabricord.Fabricord.webHookUrl
import net.rk4z.fabricord.events.*

@Suppress("unused")
@EventHandler
class DiscordPlayerEventHandler : IEventHandler {

    fun registerEventListeners() {
        ServerPlayConnectionEvents.JOIN.register(ServerPlayConnectionEvents.Join { handler: ServerPlayNetworkHandler, _: PacketSender?, _: MinecraftServer? ->
            val player = handler.player
            EventBus.postAsync(PlayerJoinEvent.get(player))
        })

        ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler: ServerPlayNetworkHandler, _: MinecraftServer? ->
            val player = handler.player
            EventBus.postAsync(PlayerLeaveEvent.get(player))
        })

        ServerMessageEvents.CHAT_MESSAGE.register(ServerMessageEvents.ChatMessage { chatmessage: SignedMessage, player: ServerPlayerEntity, _: MessageType.Parameters? ->
            val message = chatmessage.content.string
            val chatMessage = String.format("%s", message)
            EventBus.postAsync(PlayerChatEvent.get(player, chatMessage))
        })
    }

    val onPlayerJoin = handler<PlayerJoinEvent>(
        condition = { true }
    ) { event ->
        val player = event.player
        DiscordEmbed.sendPlayerJoinEmbed(player)
    }

    val onPlayerLeave = handler<PlayerLeaveEvent>(
        condition = { true }
    ) { event ->
        val player = event.player
        DiscordEmbed.sendPlayerLeftEmbed(player)
    }

    val onPlayerDeath = handler<PlayerDeathEvent>(
        condition = { true }
    ) { event ->
        val player = event.player

        val deathMessage = player.damageTracker.deathMessage.string

        DiscordEmbed.sendPlayerDeathEmbed(player, deathMessage)
    }

    val onPlayerChat = handler<PlayerChatEvent>(
        condition = { true }
    ) { event ->
        val player = event.p
        val message = event.m

        this.handleMCMessage(player, message)
    }

    val onPlayerGrantCriterion = handler<PlayerGrantCriterionEvent>(
        condition = { true }
    ) { event ->
        val player = event.player
        val criterion = event.criterion
        DiscordEmbed.sendPlayerGrantCriterionEmbed(player, criterion)
    }

    private fun handleMCMessage(player: ServerPlayerEntity, message: String) {
        when (messageStyle) {
            "modern" -> modernStyle(player, message)
            "classic" -> classicStyle(player, message)
            else -> classicStyle(player, message)
        }
    }

    private fun modernStyle(player: ServerPlayerEntity, message: String) {
        if (webHookUrl.isNullOrBlank()) {
            logger.error("Webhook URL is not configured or blank.")
            return
        }

        try {
            WebhookClient.withUrl(webHookUrl!!).use { client ->
                val builder = WebhookMessageBuilder().apply {
                    setUsername(player.name.string)
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

    private fun classicStyle(player: ServerPlayerEntity, message: String) {
        val mcId = player.name.string
        val formattedMessage = "$mcId » $message"
        DiscordBotManager.sendToDiscord(formattedMessage)
    }
}