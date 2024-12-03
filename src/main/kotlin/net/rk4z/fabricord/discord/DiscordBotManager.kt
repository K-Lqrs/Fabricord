package net.rk4z.fabricord.discord

import com.mojang.authlib.GameProfile
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.minecraft.server.BannedPlayerEntry
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.rk4z.fabricord.Fabricord
import net.rk4z.fabricord.utils.System
import net.rk4z.s1.swiftbase.core.CB
import net.rk4z.s1.swiftbase.core.LMB
import net.rk4z.s1.swiftbase.core.Logger
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*
import javax.security.auth.login.LoginException

object DiscordBotManager : ListenerAdapter() {
    internal lateinit var guild: Guild
    internal var jda: JDA? = null
    internal var webHook: Webhook? = null
    internal var botIsInitialized = false
    internal var server: MinecraftServer? = null
    internal val intents = GatewayIntent.MESSAGE_CONTENT

    fun init(s: MinecraftServer) {
        server = s
    }

    fun startBot() {
        CB.executor.execute {
            try {
                jda = JDABuilder.createDefault(Fabricord.botToken)
                    .setAutoReconnect(true)
                    .setStatus(getOnlineStatus())
                    .setActivity(getBotActivity())
                    .enableIntents(intents)
                    .addEventListeners(this)
                    .build()
                    .awaitReady()

                jda?.updateCommands()?.addCommands(
                    Commands.slash("playerlist", "Get a list of online players")
                )?.queue()

                guild = jda?.getGuildById(Fabricord.guildId!!) ?: throw IllegalStateException("Guild not found")
                botIsInitialized = true

                Logger.info("Discord bot is now online.")

                Fabricord.serverStartMessage?.let { sendToDiscord(it) }

                if (Fabricord.messageStyle == "modern") {
                    if (!Fabricord.webHookId.isNullOrBlank()) {
                        webHook = jda?.retrieveWebhookById(Fabricord.webHookId!!)?.complete()
                    } else {
                        Logger.error(LMB.getSysMessage(System.Log.WEBHOOK_NOT_CONFIGURED))
                    }
                }

            } catch (e: LoginException) {
                Logger.error(LMB.getSysMessage(System.Log.FAILED_TO_LOGIN), e)
            } catch (e: Exception) {
                Logger.error(LMB.getSysMessage(System.Log.FAILED_TO_START_BOT), e)
            }
        }
    }

    fun stopBot() {
        if (botIsInitialized) {
            Fabricord.serverStopMessage?.let { sendToDiscord(it) }
            jda?.shutdown()
            botIsInitialized = false
            Logger.info("Discord bot is now offline.")
        } else {
            Logger.error("Discord bot is not initialized.")
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.name) {
            "playerlist" -> handlePlayerListCommand(event)
        }
    }

    private fun handlePlayerListCommand(event: SlashCommandInteractionEvent) {
        val server = getServerOrReply(event) ?: return
        val onlinePlayers = server.playerManager.playerList
        val embed = EmbedBuilder()
            .setTitle("Online Players (${onlinePlayers.size})")
            .setDescription(
                if (onlinePlayers.isEmpty()) "No players online."
                else onlinePlayers.joinToString("\n") { it.name.string }
            )
            .setColor(Color.GREEN)
            .build()
        event.replyEmbeds(embed).queue()
    }

    private fun getServerOrReply(event: SlashCommandInteractionEvent): MinecraftServer? {
        return server ?: run {
            event.reply(LMB.getSysMessage(System.Log.NOT_INITIALIZED)).setEphemeral(true).queue()
            null
        }
    }

    fun sendToDiscord(message: String) {
        CB.executor.executeAsync {
            try {
                val logChannelID = Fabricord.logChannelID ?: return@executeAsync
                val channel = jda?.getTextChannelById(logChannelID) ?: return@executeAsync

                val messageAction = channel.sendMessage(message)

                val allowedMentions = mutableSetOf<Message.MentionType>().apply {
                    if (Fabricord.allowMention == true) {
                        if (Fabricord.allowEveryone == true) add(Message.MentionType.EVERYONE)
                        if (Fabricord.allowHere == true) add(Message.MentionType.HERE)
                        if (Fabricord.allowRoleMention == true) {
                            add(Message.MentionType.ROLE)
                            Fabricord.allowedRoles?.let { messageAction.mentionRoles(it) } ?: messageAction.mentionRoles(emptyList())
                        }
                        if (Fabricord.allowUserMention == true) {
                            add(Message.MentionType.USER)
                            Fabricord.allowedUsers?.let { messageAction.mentionUsers(it) } ?: messageAction.mentionUsers(emptyList())
                        }
                    }
                }

                messageAction.setAllowedMentions(allowedMentions)
                messageAction.queue()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getOnlineStatus(): OnlineStatus = when (Fabricord.botOnlineStatus?.uppercase()) {
        "ONLINE" -> OnlineStatus.ONLINE
        "IDLE" -> OnlineStatus.IDLE
        "DO_NOT_DISTURB" -> OnlineStatus.DO_NOT_DISTURB
        "INVISIBLE" -> OnlineStatus.INVISIBLE
        else -> OnlineStatus.ONLINE
    }

    private fun getBotActivity(): Activity = when (Fabricord.botActivityStatus?.lowercase()) {
        "playing" -> Activity.playing(Fabricord.botActivityMessage ?: "Minecraft Server")
        "watching" -> Activity.watching(Fabricord.botActivityMessage ?: "Minecraft Server")
        "listening" -> Activity.listening(Fabricord.botActivityMessage ?: "Minecraft Server")
        "competing" -> Activity.competing(Fabricord.botActivityMessage ?: "Minecraft Server")
        else -> Activity.playing("Minecraft Server")
    }
}
