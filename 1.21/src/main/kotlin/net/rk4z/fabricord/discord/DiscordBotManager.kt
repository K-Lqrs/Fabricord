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
import net.rk4z.beacon.EventBus
import net.rk4z.fabricord.Fabricord.botActivityMessage
import net.rk4z.fabricord.Fabricord.botActivityStatus
import net.rk4z.fabricord.Fabricord.botOnlineStatus
import net.rk4z.fabricord.Fabricord.botToken
import net.rk4z.fabricord.Fabricord.consoleLogChannelID
import net.rk4z.fabricord.Fabricord.enableConsoleLog
import net.rk4z.fabricord.Fabricord.logChannelID
import net.rk4z.fabricord.Fabricord.logger
import net.rk4z.fabricord.Fabricord.serverStartMessage
import net.rk4z.fabricord.Fabricord.serverStopMessage
import net.rk4z.fabricord.events.DiscordMCPlayerMentionEvent
import net.rk4z.fabricord.events.DiscordMessageReceiveEvent
import java.util.*
import javax.security.auth.login.LoginException

@Suppress("unused", "MemberVisibilityCanBePrivate")
object DiscordBotManager {
    var jda: JDA? = null
    var botIsInitialized: Boolean = false
    private val server: MinecraftServer? = null

    private val intents = GatewayIntent.MESSAGE_CONTENT

    fun startBot() {
        val onlineStatus = when (botOnlineStatus?.uppercase(Locale.getDefault())) {
            "ONLINE" -> OnlineStatus.ONLINE
            "IDLE" -> OnlineStatus.IDLE
            "DO_NOT_DISTURB" -> OnlineStatus.DO_NOT_DISTURB
            "INVISIBLE" -> OnlineStatus.INVISIBLE
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

        try {
            jda = JDABuilder.createDefault(botToken)
                .setAutoReconnect(true)
                .setStatus(onlineStatus)
                .setActivity(activity)
                .enableIntents(intents)
                .addEventListeners(discordListener)
                .build()
                .awaitReady()

            botIsInitialized = true
            logger.info("Discord bot is now online")
            serverStartMessage?.let { sendToDiscord(it) }
        } catch (e: LoginException) {
            logger.error("Failed to login to Discord with the provided token", e)
            logger.error(e.stackTraceToString())
        }
    }

    fun stopBot() {
        if (!botIsInitialized) {
            logger.error("Discord bot is not initialized. Cannot shutdown bot.")
            return
        }
        jda?.shutdown()
        botIsInitialized = false
        serverStopMessage?.let { sendToDiscord(it) }
        logger.info("Discord bot has been shutdown")
    }

    private val discordListener = object : ListenerAdapter() {
        override fun onMessageReceived(event: MessageReceivedEvent) {
            val server = server ?: run {
                logger.error("MinecraftServer is not initialized. Cannot process Discord message.")
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
                EventBus.callEventAsync(
                    DiscordMCPlayerMentionEvent.get(
                        event,
                        server,
                        mentionedPlayers,
                        foundUUID
                    )
                )
            } else {
                logger.info("No MCID or UUID found in the message.")
                EventBus.callEventAsync(DiscordMessageReceiveEvent.get(event, server))
            }
        }
    }


    fun sendToDiscord(message: String) {
        logChannelID?.let { jda?.getTextChannelById(it)?.sendMessage(message)?.queue() }
    }

    fun sendToDiscordForConsole(message: String) {
        if (enableConsoleLog) {
            consoleLogChannelID?.let { jda?.getTextChannelById(it)?.sendMessage("```$message```")?.queue() }
        }
    }
}