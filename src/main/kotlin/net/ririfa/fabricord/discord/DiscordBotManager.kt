package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import net.ririfa.fabricord.Config
import net.ririfa.fabricord.FT
import net.ririfa.fabricord.LM
import net.ririfa.fabricord.Logger
import net.ririfa.fabricord.Server
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.util.extractWebhookIdFromUrl
import java.util.Locale
import javax.security.auth.login.LoginException

object DiscordBotManager {
	var jda: JDA? = null
	var webHook: Webhook? = null

	var botIsInitialized = false

	private val intents = GatewayIntent.MESSAGE_CONTENT

	fun start() {
		FT {
			try {

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
			} catch (e: LoginException) {
				Logger.error(TODO())
			} catch (e: Exception) {
				Logger.error(TODO())
			}
		}
	}

	private val discordListener = object : ListenerAdapter() {
		override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {

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

	private fun validateConfigForModern() {
		if (Config.messageStyle == "modern") {
			val id = extractWebhookIdFromUrl(Config.webHookUrl)

			if (!id.isNullOrBlank()) {
				webHook = jda?.retrieveWebhookById(id)?.complete()
			} else {
				throw IllegalStateException(LM.getSysMessage(FabricordMessageKey.Exception.Config.WebHookUrlIsNotConfiguredOrInvalid))
				Server.shutdown()
			}
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
}