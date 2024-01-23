package com.inf_ruxy.projects.mc.plugin.several.fabricord.discord

import com.inf_ruxy.projects.mc.plugin.several.fabricord.Fabricord.logger
import com.inf_ruxy.projects.mc.plugin.several.fabricord.FabricordApi.config
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import java.util.*
import javax.security.auth.login.LoginException

class DiscordBotManager {

    var jda: JDA? = null

    fun startBot() {
        try {
            val token = config.botToken
            val activityType = config.botActivityStatus
            val activityMessage = config.botActivityMessage ?: "Minecraft"

            val onlineStatus = when (config.botOnlineStatus?.uppercase(Locale.getDefault())) {
                "DO_NOT_DISTURB" -> OnlineStatus.DO_NOT_DISTURB
                "INVISIBLE" -> OnlineStatus.INVISIBLE
                "IDLE" -> OnlineStatus.IDLE
                else -> OnlineStatus.ONLINE
            }

            val activity = when (activityType!!.lowercase(Locale.getDefault())) {
                "playing" -> Activity.playing(activityMessage)
                "watching" -> Activity.watching(activityMessage)
                "listening" -> Activity.listening(activityMessage)
                "competing" -> Activity.competing(activityMessage)
                else -> Activity.playing(activityMessage)
            }

            jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
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

    private val discordListener = object : ListenerAdapter() {
        override fun onMessageReceived(event: MessageReceivedEvent) {
            TODO("Discordからのメッセージを処理するやつ")
        }
    }

    fun sendToDiscord(message: String) {
        val channelId = config.logChannelID!!
        val channel = channelId.let { jda?.getTextChannelById(it) }
        channel!!.sendMessage(message).queue()
    }

}