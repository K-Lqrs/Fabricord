package net.rk4z.fabricord.discord

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.rk4z.fabricord.Fabricord.botActivityMessage
import net.rk4z.fabricord.Fabricord.botActivityStatus
import net.rk4z.fabricord.Fabricord.botOnlineStatus
import net.rk4z.fabricord.Fabricord.botToken
import net.rk4z.fabricord.Fabricord.logChannelID
import net.rk4z.fabricord.Fabricord.logger
import net.rk4z.fabricord.Fabricord.serverStartMessage
import net.rk4z.fabricord.Fabricord.serverStopMessage
import java.util.*
import javax.security.auth.login.LoginException

object DiscordBotManager {
    var jda: JDA? = null
    var botIsInitialized: Boolean = false

    private val intents = GatewayIntent.MESSAGE_CONTENT

    fun startBot() {
        val onlineStatus = when (botOnlineStatus?.uppercase(Locale.getDefault()))  {
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

    fun sendToDiscord(message: String) {
        logChannelID?.let { jda?.getTextChannelById(it)?.sendMessage(message)?.queue() }
    }
}