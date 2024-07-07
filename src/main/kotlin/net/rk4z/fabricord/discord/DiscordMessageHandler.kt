package net.rk4z.fabricord.discord

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.server.MinecraftServer
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.rk4z.beacon.EventHandler
import net.rk4z.beacon.IEventHandler
import net.rk4z.beacon.handler
import net.rk4z.fabricord.Fabricord.logChannelID
import net.rk4z.fabricord.events.DiscordMCPlayerMentionEvent
import net.rk4z.fabricord.events.DiscordMessageReceiveEvent
import net.rk4z.fabricord.util.Utils.replaceUUIDsWithMCIDs
import java.awt.Color

@Suppress("unused")
@EventHandler
class DiscordMessageHandler(s: MinecraftServer) : IEventHandler {
    private val registryManager = s.registryManager

    val handleDiscordMessage = handler<DiscordMessageReceiveEvent>(
        condition = { true }
    ) { event ->
        val message: Text? = createMessage(event.c, false, null)

        event.s.playerManager.playerList.forEach { player ->
            player.sendMessage(message, false)
        }
    }

    val handleMentionedDiscordMessage = handler<DiscordMCPlayerMentionEvent>(
        condition = { true }
    ) { event ->
        val updatedMessageContent = replaceUUIDsWithMCIDs(event.c.message.contentRaw, event.s.playerManager.playerList)

        event.m.forEach { player ->
            player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
        }

        val mentionMessage =
            createMessage(event.c, true, if (event.fu) updatedMessageContent.first else event.c.message.contentRaw)
        val generalMessage =
            createMessage(event.c, false, if (event.fu) updatedMessageContent.first else event.c.message.contentRaw)

        event.m.forEach { player ->
            player.sendMessage(mentionMessage, false)
        }

        val nonMentionedPlayers = if (event.fu) updatedMessageContent.second else event.m
        event.s.playerManager.playerList.forEach { player ->
            if (player !in nonMentionedPlayers) {
                player.sendMessage(generalMessage, false)
            }
        }
    }

    private fun createMessage(event: MessageReceivedEvent, isMention: Boolean, updatedContent: String?): Text? {
        val channelId: String = logChannelID!!
        if (event.channel.id != channelId || event.author.isBot) {
            return null
        }

        val member = event.member
        val memberName = member?.user?.name ?: "Unknown Name"
        val memberId = member?.user?.id ?: "00000000000000000000"
        val idSuggest = "<@$memberId>"
        val highestRole = member?.roles?.maxByOrNull { it.position }
        val roleName = highestRole?.name
        val rIdSuggest = highestRole?.id?.let { "<@&$it>" }
        val roleColor = highestRole?.color ?: Color.WHITE
        val kyoriRoleColor = TextColor.color(roleColor.red, roleColor.green, roleColor.blue)

        var componentMessage = Component.text("[", TextColor.color(0xFFFFFF))
            .append(Component.text("Discord", TextColor.color(0x55CDFC)))

        componentMessage = if (roleName != null) {
            componentMessage.append(Component.text(" | ", TextColor.color(0xFFFFFF)))
                .append(
                    Component.text(roleName, kyoriRoleColor)
                        .clickEvent(ClickEvent.suggestCommand(rIdSuggest!!))
                )
                .append(Component.text("]", TextColor.color(0xFFFFFF)))
                .append(Component.text(" "))
        } else {
            componentMessage.append(Component.text("]", TextColor.color(0xFFFFFF)))
                .append(Component.text(" "))
        }

        componentMessage = componentMessage.append(
            Component.text(memberName)
                .clickEvent(ClickEvent.suggestCommand(idSuggest))
        )

        val messageContent = if (isMention) {
            Component.text(" » " + (updatedContent ?: event.message.contentDisplay)).decorate(TextDecoration.BOLD)
        } else {
            Component.text(" » " + (updatedContent ?: event.message.contentDisplay))
        }

        componentMessage = componentMessage.append(messageContent)

        val json = GsonComponentSerializer.gson().serialize(componentMessage)
        return Text.Serialization.fromJson(json, registryManager)
    }

}