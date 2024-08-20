package net.rk4z.fabricord.discord

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.rk4z.fabricord.Fabricord
import net.rk4z.fabricord.Fabricord.executorService
import java.util.*
import javax.security.auth.login.LoginException

object DiscordBotManager {
    var jda: JDA? = null
    private var botIsInitialized: Boolean = false
    var server: MinecraftServer? = null

    private val intents = GatewayIntent.MESSAGE_CONTENT

    fun init(s: MinecraftServer) {
        server = s
    }

    fun startBot() {
        val onlineStatus = when (Fabricord.botOnlineStatus?.uppercase(Locale.getDefault())) {
            "ONLINE" -> OnlineStatus.ONLINE
            "IDLE" -> OnlineStatus.IDLE
            "DO_NOT_DISTURB" -> OnlineStatus.DO_NOT_DISTURB
            "INVISIBLE" -> OnlineStatus.INVISIBLE
            null -> OnlineStatus.ONLINE
            else -> OnlineStatus.ONLINE
        }

        val activity = when (Fabricord.botActivityStatus?.lowercase(Locale.getDefault())) {
            "playing" -> Fabricord.botActivityMessage?.let { Activity.playing(it) }
            "watching" -> Fabricord.botActivityMessage?.let { Activity.watching(it) }
            "listening" -> Fabricord.botActivityMessage?.let { Activity.listening(it) }
            "competing" -> Fabricord.botActivityMessage?.let { Activity.competing(it) }

            // Similar measures have been taken here.
            null -> Activity.playing("Minecraft Server")

            else -> Fabricord.botActivityMessage?.let { Activity.playing(it) }
        }

        try {
            jda = JDABuilder.createDefault(Fabricord.botToken)
                .setAutoReconnect(true)
                .setStatus(onlineStatus)
                .setActivity(activity)
                .enableIntents(intents)
                .addEventListeners(discordListener)
                .build()
                .awaitReady()

            botIsInitialized = true
            Fabricord.logger.info("Discord bot is now online")
            Fabricord.serverStartMessage?.let { sendToDiscord(it) }
        } catch (e: LoginException) {
            Fabricord.logger.error("Failed to login to Discord with the provided token", e)
            Fabricord.logger.error(e.stackTraceToString())
        }
    }

    fun stopBot() {
        if (botIsInitialized) {
            Fabricord.logger.info("Discord bot is now offline")
            Fabricord.serverStopMessage?.let { sendToDiscord(it) }
            jda?.shutdown()
            botIsInitialized = false
        } else {
            Fabricord.logger.error("Discord bot is not initialized. Cannot stop the bot.")
        }
    }

    private val discordListener = object : ListenerAdapter() {
        override fun onMessageReceived(event: MessageReceivedEvent) {
            val server = server ?: run {
                Fabricord.logger.error("MinecraftServer is not initialized. Cannot process Discord message.")
                return
            }

            val messageContent = event.message.contentRaw
            val players = server.playerManager.playerList

            var foundMCID = false
            var foundUUID = false
            val mentionedPlayers = mutableListOf<ServerPlayerEntity>()

            val mcidPattern = Regex("@([a-zA-Z0-9_]+)")
            val mcidMatches = mcidPattern.findAll(messageContent)
            mcidMatches.forEach { match ->
                val mcid = match.groupValues[1]
                val player = players.find { it.name.string == mcid }
                player?.let {
                    foundMCID = true
                    mentionedPlayers.add(it)
                }
            }

            val uuidPattern = Regex("@\\{([0-9a-fA-F-]+)}")
            val uuidMatches = uuidPattern.findAll(messageContent)
            uuidMatches.forEach { match ->
                val uuidStr = match.groupValues[1]
                val player = players.find { it.uuid.toString() == uuidStr }
                player?.let {
                    foundUUID = true
                    mentionedPlayers.add(it)
                }
            }

            if (foundMCID || foundUUID) {
                DiscordMessageHandler.handleMentionedDiscordMessage(event, server, mentionedPlayers, foundUUID)
            } else {
                DiscordMessageHandler.handleDiscordMessage(event, server)
            }
        }
    }

    fun sendToDiscord(message: String) {
        executorService.submit {
            Fabricord.logChannelID?.let { jda?.getTextChannelById(it)?.sendMessage(message)?.queue() }
        }
    }

}