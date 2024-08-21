package net.rk4z.fabricord.discord

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.rk4z.fabricord.Fabricord
import net.rk4z.fabricord.Fabricord.executorService
import java.awt.Color
import java.util.*
import javax.security.auth.login.LoginException

object DiscordBotManager : ListenerAdapter() {
    var jda: JDA? = null
    private var server: MinecraftServer? = null

    var botIsInitialized: Boolean = false
    private val intents = GatewayIntent.MESSAGE_CONTENT

    fun init(s: MinecraftServer) {
        server = s
    }

    fun startBot() {
        executorService.submit {
            val onlineStatus = getOnlineStatus()
            val activity = getBotActivity()

            try {
                jda = JDABuilder.createDefault(Fabricord.botToken)
                    .setAutoReconnect(true)
                    .setStatus(onlineStatus)
                    .setActivity(activity)
                    .enableIntents(intents)
                    .addEventListeners(discordListener, this)
                    .build()
                    .awaitReady()

                jda?.updateCommands()?.addCommands(
                    Commands.slash("playerlist", "Get a list of online players")
                )?.queue()

                jda?.updateCommands()

                botIsInitialized = true
                Fabricord.logger.info("Discord bot is now online")
                Fabricord.serverStartMessage?.let { sendToDiscord(it) }
            } catch (e: LoginException) {
                Fabricord.logger.error("Failed to login to Discord with the provided token", e)
            } catch (e: Exception) {
                Fabricord.logger.error("An unexpected error occurred during Discord bot startup", e)
            }
        }
    }

    fun stopBot() {
        if (botIsInitialized) {
            Fabricord.serverStopMessage?.let { sendToDiscord(it) }
            Fabricord.logger.info("Discord bot is now offline")
            jda?.shutdown()
            botIsInitialized = false
        } else {
            Fabricord.logger.error("Discord bot is not initialized. Cannot stop the bot.")
        }
    }

    private fun getOnlineStatus(): OnlineStatus {
        return when (Fabricord.botOnlineStatus?.uppercase(Locale.getDefault())) {
            "ONLINE" -> OnlineStatus.ONLINE
            "IDLE" -> OnlineStatus.IDLE
            "DO_NOT_DISTURB" -> OnlineStatus.DO_NOT_DISTURB
            "INVISIBLE" -> OnlineStatus.INVISIBLE
            else -> OnlineStatus.ONLINE
        }
    }

    private fun getBotActivity(): Activity {
        return when (Fabricord.botActivityStatus?.lowercase(Locale.getDefault())) {
            "playing" -> Activity.playing(Fabricord.botActivityMessage ?: "Minecraft Server")
            "watching" -> Activity.watching(Fabricord.botActivityMessage ?: "Minecraft Server")
            "listening" -> Activity.listening(Fabricord.botActivityMessage ?: "Minecraft Server")
            "competing" -> Activity.competing(Fabricord.botActivityMessage ?: "Minecraft Server")
            else -> Activity.playing("Minecraft Server")
        }
    }

//>------------------------------------------------------------------------------------------------------------------<\\

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.name) {
            "playerlist" -> handlePlayerListCommand(event)
        }
    }

    private val discordListener = object : ListenerAdapter() {
        override fun onMessageReceived(event: MessageReceivedEvent) {
            val server = server ?: return Fabricord.logger.error("MinecraftServer is not initialized. Cannot process Discord message.")

            executorService.submit {
                val mentionedPlayers = findMentionedPlayers(event.message.contentRaw, server.playerManager.playerList)
                if (mentionedPlayers.isNotEmpty()) {
                    DiscordMessageHandler.handleMentionedDiscordMessage(event, server, mentionedPlayers, false)
                } else {
                    DiscordMessageHandler.handleDiscordMessage(event, server)
                }
            }
        }
    }

    private fun handlePlayerListCommand(event: SlashCommandInteractionEvent) {
        val server = server ?: return Fabricord.logger.error("MinecraftServer is not initialized. Cannot process Discord command.")

        val onlinePlayers = server.playerManager.playerList
        val playerCount = onlinePlayers.size

        val embedBuilder = EmbedBuilder()
            .setTitle("Online Players")
            .setColor(Color.GREEN)
            .setDescription("現在オンラインのプレイヤーは $playerCount 人です。\n")

        if (playerCount > 0) {
            val playerList = onlinePlayers.joinToString(separator = "\n") { player -> player.name.string }
            embedBuilder.setDescription(embedBuilder.descriptionBuilder.append(playerList).toString())
        } else {
            embedBuilder.setDescription("現在オンラインのプレイヤーはいません。")
        }

        event.replyEmbeds(embedBuilder.build()).queue()
    }

    private fun findMentionedPlayers(messageContent: String, players: List<ServerPlayerEntity>): List<ServerPlayerEntity> {
        val mentionedPlayers = mutableListOf<ServerPlayerEntity>()
        val mcidPattern = Regex("@([a-zA-Z0-9_]+)")
        val uuidPattern = Regex("@\\{([0-9a-fA-F-]+)}")

        mcidPattern.findAll(messageContent).forEach { match ->
            val mcid = match.groupValues[1]
            players.find { it.name.string == mcid }?.let { mentionedPlayers.add(it) }
        }

        uuidPattern.findAll(messageContent).forEach { match ->
            val uuidStr = match.groupValues[1]
            players.find { it.uuid.toString() == uuidStr }?.let { mentionedPlayers.add(it) }
        }

        return mentionedPlayers
    }

    fun sendToDiscord(message: String) {
        executorService.submit {
            Fabricord.logChannelID?.let { jda?.getTextChannelById(it)?.sendMessage(message)?.queue() }
        }
    }
}
