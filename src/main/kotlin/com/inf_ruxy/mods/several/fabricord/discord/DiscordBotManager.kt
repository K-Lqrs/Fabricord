package com.inf_ruxy.mods.several.fabricord.discord

import com.inf_ruxy.mods.several.fabricord.Fabricord.logger
import com.inf_ruxy.mods.several.fabricord.FabricordApi.config
import com.inf_ruxy.mods.several.fabricord.FabricordApi.discordConsoleCommandListener
import com.inf_ruxy.mods.several.fabricord.FabricordApi.discordEmbed
import com.inf_ruxy.mods.several.fabricord.FabricordApi.dml
import com.inf_ruxy.mods.several.fabricord.FabricordApi.mcml

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents

import net.minecraft.network.message.MessageType
import net.minecraft.network.message.SignedMessage

import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity

import java.util.*
import javax.security.auth.login.LoginException


class DiscordBotManager {

    var jda: JDA? = null

    fun startBot() {
        try {
            val token = config.botToken
            val activityType = config.botActivityStatus
            val activityMessage = config.botActivityMessage

            if (config.botActivityMessage.isNullOrBlank()) {
                config.botActivityMessage = "Minecraft Server"
            }

            if (config.botActivityStatus.isNullOrBlank()) {
                config.botActivityStatus = "playing"
            }

            val onlineStatus = when (config.botOnlineStatus?.uppercase(Locale.getDefault())) {
                "DO_NOT_DISTURB" -> OnlineStatus.DO_NOT_DISTURB
                "INVISIBLE" -> OnlineStatus.INVISIBLE
                "IDLE" -> OnlineStatus.IDLE
                null -> OnlineStatus.ONLINE
                else -> OnlineStatus.ONLINE
            }

            val activity = when (activityType?.lowercase(Locale.getDefault())) {
                "playing" -> activityMessage?.let { Activity.playing(it) }
                "watching" -> activityMessage?.let { Activity.watching(it) }
                "listening" -> activityMessage?.let { Activity.listening(it) }
                "competing" -> activityMessage?.let { Activity.competing(it) }
                null -> Activity.playing("Minecraft Server")
                else -> activityMessage?.let { Activity.playing(it) }
            }

            jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .setStatus(onlineStatus)
                .setActivity(activity)
                .addEventListeners(discordListener)
                .build()
                .awaitReady()

            logger.info("The Discord Bot has been successfully activated.")
            sendToDiscord(":white_check_mark: **Server has Started!**")

        } catch (e: LoginException) {
            logger.error("Cannot start the bot. Please check your bot token.\n" + e.message)
        }

    }

    fun stopBot() {
        try {
            jda?.shutdown()
            logger.info("The Discord Bot has been successfully deactivated.")
            sendToDiscord(":octagonal_sign: **Server has Stopped!**")
        } catch (e: Exception) {
            logger.error("Bot could not be terminated successfully.\n" + e.message)
        }
    }

    private val discordListener = object : ListenerAdapter() {
        override fun onMessageReceived(event: MessageReceivedEvent) {
            dml.handleDiscordMessage(event)
            discordConsoleCommandListener.onMessageReceived(event)
        }
    }

    fun registerEventListeners() {
        ServerPlayConnectionEvents.JOIN.register(ServerPlayConnectionEvents.Join { handler: ServerPlayNetworkHandler, _: PacketSender?, _: MinecraftServer? ->
            val player = handler.player
            discordEmbed.sendPlayerJoinEmbed(player)
        })

        ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler: ServerPlayNetworkHandler, _: MinecraftServer? ->
            val player = handler.player
            discordEmbed.sendPlayerLeftEmbed(player)
        })

        ServerMessageEvents.CHAT_MESSAGE.register(ServerMessageEvents.ChatMessage { chatmessage: SignedMessage, player: ServerPlayerEntity, _: MessageType.Parameters? ->
            val sender = player.displayName.string
            val message = chatmessage.content.string
            val chatMessage = String.format("%s » %s", sender, message)
            mcml.handleMCMessage(player, sender, chatMessage)
        })

    }

    fun sendToDiscord(message: String) {
        val channelId = config.logChannelID!!
        val channel = channelId.let { jda?.getTextChannelById(it) }
        channel!!.sendMessage(message).queue()
    }

}