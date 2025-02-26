package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.ririfa.fabricord.Config
import net.ririfa.fabricord.FT
import net.ririfa.fabricord.Server
import net.ririfa.fabricord.util.replaceUUIDsWithMCIDs
import java.awt.Color

object DiscordMessageHandler {
	fun handleDiscordMessage(event: MessageReceivedEvent) {
		FT {
			val message: Text = createMessage(event, false, null) ?: return@FT
			sendToAllPlayers(Server, message)
		}
	}

	fun handleMentionedDiscordMessage(event: MessageReceivedEvent, mentionedPlayers: List<ServerPlayerEntity>, foundUUID: Boolean) {
		FT {
			val updatedMessageContent = replaceUUIDsWithMCIDs(event.message.contentRaw, Server.playerManager.playerList)

			mentionedPlayers.forEach { player ->
				player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_PLING.comp_349(), SoundCategory.MASTER, 2.0f, 2.0f)
			}

			val mentionMessage: Text =
				createMessage(event, true, if (foundUUID) updatedMessageContent.first else event.message.contentRaw) ?: return@FT
			val generalMessage: Text =
				createMessage(event, false, if (foundUUID) updatedMessageContent.first else event.message.contentRaw) ?: return@FT

			mentionedPlayers.forEach { player ->
				player.sendMessage(mentionMessage, false)
			}

			val nonMentionedPlayers = if (foundUUID) updatedMessageContent.second else mentionedPlayers
			sendToAllPlayersExcept(Server, generalMessage, nonMentionedPlayers)
		}
	}

	private fun sendToAllPlayers(server: MinecraftServer, message: Text) {
		server.playerManager.playerList.forEach { player ->
			player.sendMessage(message, false)
		}
	}

	private fun sendToAllPlayersExcept(server: MinecraftServer, message: Text, excludePlayers: List<ServerPlayerEntity>) {
		server.playerManager.playerList.forEach { player ->
			if (player !in excludePlayers) {
				player.sendMessage(message, false)
			}
		}
	}

	private fun createMessage(event: MessageReceivedEvent, isMention: Boolean, updatedContent: String?): Text? {
		val channelId: String = Config.logChannelID
		if (event.channel.id != channelId || event.author.isBot) {
			return null
		}

		val guildName = event.guild.name
		val member = event.guild.getMember(event.author)
		val memberName = member?.effectiveName ?: member?.user?.globalName ?: member?.user?.name ?: "Unknown"
		val memberId = member?.user?.id ?: "00000000000000000000"
		val idSuggest = "<@$memberId>"
		val highestRole = member?.roles?.maxByOrNull { it.position }
		val roleName = highestRole?.name
		val rId = highestRole?.id ?: "00000000000000000000"
		val rIdSuggest = rId.let { "<@&$it>" }
		val roleColor = highestRole?.color ?: Color.WHITE
		val roleTextColor = TextColor.fromRgb((roleColor.red shl 16) or (roleColor.green shl 8) or roleColor.blue)

		val discordText = Text.literal("Discord")
			.styled {
				it.withColor(0x55CDFC)
					.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(guildName))) }

		val roleText = roleName?.let {
			Text.literal(" | $it")
				.styled {
					it.withColor(roleTextColor)
						.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(rId)))
						.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, rIdSuggest))
				}
		} ?: Text.literal("")

		val memberText = Text.literal(" $memberName")
			.styled {
				it.withColor(0x55CDFC)
					.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(memberId)))
					.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, idSuggest))
			}

		val messageContent = updatedContent ?: event.message.contentDisplay
		val messageText = if (isMention) {
			Text.literal(" » $messageContent").styled { it.withBold(true) }
		} else {
			Text.literal(" » $messageContent")
		}

		return Text.empty()
			.append(Text.literal("[").styled { it.withColor(0xFFFFFF) })
			.append(discordText)
			.append(Text.of(" | "))
			.append(roleText)
			.append(Text.literal("] ").styled { it.withColor(0xFFFFFF) })
			.append(memberText)
			.append(messageText)
	}

}