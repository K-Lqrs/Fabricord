package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import net.minecraft.server.network.ServerPlayerEntity
import net.ririfa.fabricord.*
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.util.extractWebhookIdFromUrl
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import javax.security.auth.login.LoginException

object DiscordBotManager {
	var jda: JDA? = null
	var webHook: Webhook? = null

	var botIsInitialized = false

	private val intents = GatewayIntent.MESSAGE_CONTENT
	private val logQueue = ConcurrentLinkedQueue<String>()
	private val mcidPattern = Regex("@([a-zA-Z0-9_]+)")
	private val uuidPattern = Regex("@\\{([0-9a-fA-F-]+)}")

	init {
		FT(delay = 0, period = 2000, unit = TimeUnit.MILLISECONDS, newThread = true) {
			flushLogQueue()
		}
	}

	fun start() {
		FT {
			try {
				if (!validateConfigForModern()) {
					Logger.error(LM.getSysMessage(FabricordMessageKey.Exception.Config.WebHookUrlIsNotConfiguredOrInvalid))
					return@FT
				}

				jda = JDABuilder.createDefault(Config.botToken)
					.addEventListeners(discordListener)
					.setStatus(onlineStatus())
					.setActivity(activity())
					.setAutoReconnect(true)
					.enableIntents(intents)
					.build()
					.awaitReady()

				jda?.updateCommands()?.addCommands(
					Commands.slash("playerlist", "Get a list of online players")
				)?.queue()

				botIsInitialized = true
				Logger.info(LM.getSysMessage(TODO(), jda?.selfUser?.name ?: "Bot"))
				Config.serverStartMessage?.let { sendToDiscord(it) }

			} catch (e: LoginException) {
				Logger.error(TODO())
			} catch (e: Exception) {
				Logger.error(TODO())
			}
		}
	}

	fun stop() {
		FT {
			Config.serverStopMessage?.let { sendToDiscord(it) }

			try {
				jda?.shutdown()
				botIsInitialized = false
				Logger.info(TODO())
			} catch (e: Exception) {
				Logger.error(TODO())
				e.printStackTrace()
			}
		}
	}

	private val discordListener = object : ListenerAdapter() {
		override fun onMessageReceived(event: MessageReceivedEvent) {
			if (event.channel == Config.logChannelID.let { jda?.getTextChannelById(it) }) {
				FT {
					val mentionedPlayers = findMentionedPlayers(event.message.contentRaw, Server.playerManager.playerList)
					if (mentionedPlayers.isNotEmpty()) {
						DiscordMessageHandler.handleMentionedDiscordMessage(event, mentionedPlayers, false)
					} else {
						DiscordMessageHandler.handleDiscordMessage(event)
					}
				}
			} else if (event.channel == Config.consoleLogChannelID?.let { jda?.getTextChannelById(it) }) {
				if (!event.author.isBot) {
					val command = event.message.contentRaw
					Server.execute {
						Server.commandManager.executeWithPrefix(Server.commandSource, command)
					}
				}
			}
		}
	}

	private fun onlineStatus(): OnlineStatus {
		return when (Config.botActivityStatus?.lowercase(Locale.getDefault())) {
			"online" -> OnlineStatus.ONLINE
			"idle" -> OnlineStatus.IDLE
			"dnd" -> OnlineStatus.DO_NOT_DISTURB
			"invisible" -> OnlineStatus.INVISIBLE
			else -> OnlineStatus.ONLINE
		}
	}

	private fun activity(): Activity {
		val activityStatus = Config.botActivityStatus
		val activityMessage = Config.botActivityMessage

		return when (activityStatus?.lowercase(Locale.getDefault())) {
			"playing" -> Activity.playing(activityMessage!!)
			"watching" -> Activity.watching(activityMessage!!)
			"listening" -> Activity.listening(activityMessage!!)
			"competing" -> Activity.competing(activityMessage!!)
			else -> Activity.playing(activityMessage!!)
		}
	}

	private fun validateConfigForModern(): Boolean {
		if (Config.messageStyle == "modern") {
			val id = extractWebhookIdFromUrl(Config.webHookUrl)

			return if (!id.isNullOrBlank()) {
				jda?.retrieveWebhookById(id)?.queue { webhook ->
					webHook = webhook
				}
				webHook != null
			} else {
				false
			}
		}
		return true
	}

	private fun findMentionedPlayers(messageContent: String, players: List<ServerPlayerEntity>): List<ServerPlayerEntity> {
		val mentionedPlayers = mutableListOf<ServerPlayerEntity>()

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

	private fun flushLogQueue() {
		if (logQueue.isEmpty()) return

		val messages = mutableListOf<String>()
		var msg: String?

		while (logQueue.poll().also { msg = it } != null) {
			messages.add(msg!!)
		}

		val combinedMessage = messages.joinToString("\n")

		Config.consoleLogChannelID?.let { channelId ->
			jda?.getTextChannelById(channelId)?.sendMessage(combinedMessage)
				?.setAllowedMentions(emptySet())
				?.queue()
		}
	}

	fun sendToDiscord(message: String) {
		FT {
			Config.logChannelID.let {
				val messageAction = jda?.getTextChannelById(it)?.sendMessage(message)
				if (Config.allowMentions == false) {
					messageAction?.setAllowedMentions(emptySet())
				}
				messageAction?.queue()
			}
		}
	}

	fun sendToDiscordConsole(message: String) {
		logQueue.add(message)
	}
}