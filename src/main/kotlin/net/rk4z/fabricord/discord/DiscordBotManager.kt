package net.rk4z.fabricord.discord

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
import net.dv8tion.jda.api.requests.GatewayIntent
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.rk4z.fabricord.DataManager
import net.rk4z.fabricord.Fabricord
import net.rk4z.fabricord.utils.System
import net.rk4z.fabricord.utils.Utils.linkCodeCache
import net.rk4z.s1.swiftbase.core.CB
import net.rk4z.s1.swiftbase.core.LMB
import net.rk4z.s1.swiftbase.core.Logger
import java.awt.Color
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
                    .addEventListeners(this, discordListener)
                    .build()
                    .awaitReady()

                jda?.updateCommands()?.addCommands(
                    Commands.slash("playerlist", "Get a list of online players"),
                    Commands.slash("link", "Link your Minecraft account with Discord")
                        .addOption(OptionType.STRING, "code", "The code you received in Minecraft", true)
                )?.queue()

                guild = jda?.getGuildById(Fabricord.guildId!!) ?: throw IllegalStateException(LMB.getSysMessage(System.Log.Bot.GUILD_NOT_FOUND))
                botIsInitialized = true

                Logger.info(LMB.getSysMessage(System.Log.Bot.BOT_ONLINE))

                Fabricord.serverStartMessage?.let { sendToDiscord(it) }

                if (Fabricord.messageStyle == "modern") {
                    if (!Fabricord.webHookId.isNullOrBlank()) {
                        webHook = jda?.retrieveWebhookById(Fabricord.webHookId!!)?.complete()
                    } else {
                        Logger.error(LMB.getSysMessage(System.Log.Bot.WEBHOOK_NOT_CONFIGURED))
                    }
                }

            } catch (e: LoginException) {
                Logger.error(LMB.getSysMessage(System.Log.Bot.FAILED_TO_LOGIN), e)
            } catch (e: Exception) {
                Logger.error(LMB.getSysMessage(System.Log.Bot.FAILED_TO_START_BOT), e)
            }
        }
    }

    fun stopBot() {
        if (botIsInitialized) {
            Fabricord.serverStopMessage?.let { sendToDiscord(it) }
            jda?.shutdown()
            botIsInitialized = false
            Logger.info(LMB.getSysMessage(System.Log.Bot.BOT_OFFLINE))
        } else {
            Logger.error(LMB.getSysMessage(System.Log.Bot.BOT_NOT_INITIALIZED))
        }
    }

    private val discordListener = object : ListenerAdapter() {
        override fun onMessageReceived(event: MessageReceivedEvent) {
            val server = server ?: return Logger.error(LMB.getSysMessage(System.Log.SERVER_NOT_INITIALIZED))

            CB.executor.executeAsync {
                val mentionedPlayers = findMentionedPlayers(event.message.contentRaw, server.playerManager.playerList)
                if (mentionedPlayers.isNotEmpty()) {
                    DiscordMessageHandler.handleMentionedDiscordMessage(event, server, mentionedPlayers, false)
                } else {
                    DiscordMessageHandler.handleDiscordMessage(event, server)
                }
            }
        }
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

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.name) {
            "playerlist" -> handlePlayerListCommand(event)
            "link" -> handleLinkCommand(event)
        }
    }

    private fun handlePlayerListCommand(event: SlashCommandInteractionEvent) {
        var lang: String = event.guildLocale.locale
        if (lang == "unknown") {
            // Default fallback
            lang = "en"
        }
        Fabricord.get()?.availableLang?.let {
            if (lang !in it) {
                lang = "en"
            }
        }
        val server = getServerOrReply(event) ?: return

        val onlinePlayers = server.playerManager.playerList
        val playerCount = onlinePlayers.size

        // Discord API doesn't provide user language.
        val embedBuilder = EmbedBuilder()
            .setTitle(LMB.getSysMessageByLangCode(System.Command.Online_Players.TITLE, lang))
            .setColor(Color.GREEN)
            .setDescription(LMB.getSysMessageByLangCode(
                key = System.Command.Online_Players.DESCRIPTION,
                lang = lang,
                args = arrayOf(playerCount)
            ))

        if (playerCount > 0) {
            val playerList = onlinePlayers.joinToString(separator = "\n") { player -> player.name.string }
            embedBuilder.setDescription(embedBuilder.descriptionBuilder.append(playerList).toString())
        } else {
            embedBuilder.setDescription(LMB.getSysMessageByLangCode(System.Command.Online_Players.NO_PLAYER, lang))
        }

        event.replyEmbeds(embedBuilder.build()).queue()
    }

    private fun handleLinkCommand(event: SlashCommandInteractionEvent) {
        val discordUserID = event.user.id
        val linkCode = event.getOption("code")?.asString ?: return event.reply("Invalid code").setEphemeral(true).queue { t ->
            t.deleteOriginal().queueAfter(5, java.util.concurrent.TimeUnit.SECONDS)
        }

        // ShortUUIDStringが返ってくる
        val cachedValue = linkCodeCache.getIfPresent(linkCode)

        if (cachedValue != null) {
            val userLang = DataManager.getPlayerData(cachedValue).lang
            DataManager.insertLinkData(cachedValue, discordUserID)

            event.reply(LMB.getSysMessageByLangCode(System.Command.Link.SUCCESS, userLang))
                .setEphemeral(true)
                .queue { t ->
                    t.deleteOriginal().queueAfter(5, java.util.concurrent.TimeUnit.SECONDS)
                }
        } else {
            event.reply("Invalid code. Please try again.")
                .setEphemeral(true)
                .queue { t ->
                    t.deleteOriginal().queueAfter(5, java.util.concurrent.TimeUnit.SECONDS)
                }
        }
    }

    private fun getServerOrReply(event: SlashCommandInteractionEvent): MinecraftServer? {
        return server ?: run {
            event.reply(LMB.getSysMessage(System.Log.SERVER_NOT_INITIALIZED)).setEphemeral(true).queue()
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
