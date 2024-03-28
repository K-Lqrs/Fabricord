package com.inf_ruxy.fabricord.discord

import com.inf_ruxy.fabricord.Fabricord.Companion.logger
import com.inf_ruxy.fabricord.FabricordApi.discordEmbed
import com.inf_ruxy.fabricord.FabricordApi.discordMessageListener
import com.inf_ruxy.fabricord.FabricordApi.mcMessageListener
import com.inf_ruxy.fabricord.util.ConfigManager.Companion.botActivityMessage
import com.inf_ruxy.fabricord.util.ConfigManager.Companion.botActivityStatus
import com.inf_ruxy.fabricord.util.ConfigManager.Companion.botOnlineStatus
import com.inf_ruxy.fabricord.util.ConfigManager.Companion.botToken
import com.inf_ruxy.fabricord.util.ConfigManager.Companion.logChannelID
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

    /**
     * Starts the Discord Bot.
     *
     * This method initializes the Discord Bot by setting the online status and activity based on the provided settings.
     * It then creates and configures a JDABuilder object using the bot token, online status, activity, and event listeners.
     * Finally, it builds and awaits the JDA instance, logs the successful activation, and sends a message to the Discord channel.
     * If any exception occurs during the process, it logs an error message with the corresponding exception stack trace.
     *
     * @throws LoginException if failed to login to Discord with the provided bot token.
     * @throws Exception if an error occurs while starting the Discord bot.
     */
    fun startBot() {
        try {

            val onlineStatus = when (botOnlineStatus?.uppercase(Locale.getDefault())) {
                "DO_NOT_DISTURB" -> OnlineStatus.DO_NOT_DISTURB
                "INVISIBLE" -> OnlineStatus.INVISIBLE
                "IDLE" -> OnlineStatus.IDLE

                // Because of the null check in advance,
                // nulls are definitely not generated at this point,
                // but the behaviour in case of nulls is also implemented for unforeseen situations.
                null -> OnlineStatus.ONLINE

                else -> OnlineStatus.ONLINE
            }

            val activity = when (botActivityStatus?.lowercase(Locale.getDefault())) {
                "playing" -> botActivityMessage?.let { Activity.playing(it) }
                "watching" -> botActivityMessage?.let { Activity.watching(it) }
                "listening" -> botActivityMessage?.let { Activity.listening(it) }
                "competing" -> botActivityMessage?.let { Activity.competing(it) }

                // Similar measures have been taken here.
                null -> Activity.playing("Minecraft Server")

                else -> botActivityMessage?.let { Activity.playing(it) }
            }

            jda = JDABuilder.createDefault(botToken)
                .setAutoReconnect(true)
                .setStatus(onlineStatus)
                .setActivity(activity)
                .addEventListeners(discordListener)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build()
                .awaitReady()

            logger.info("The Discord Bot has been successfully activated.")
            sendToDiscord(":white_check_mark: **Server has Started!**")

        } catch (e: LoginException) {
            logger.error("Failed to login to Discord. Please check your bot token.", e)
            logger.error(e.stackTraceToString())
        } catch (e: Exception) {
            logger.error("An error occurred while starting the Discord bot.", e)
            logger.error(e.stackTraceToString())
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
            discordMessageListener.handleDiscordMessage(event)
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
            val chatMessage = String.format("%s", message)
            mcMessageListener.handleMCMessage(player, sender, chatMessage)
        })
    }

    fun sendToDiscord(message: String) {
        val channelId = logChannelID!!
        val channel = channelId.let { jda?.getTextChannelById(it) }
        channel!!.sendMessage(message).queue()
    }

}