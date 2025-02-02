package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.ririfa.fabricord.Fabricord
import net.ririfa.fabricord.utils.Utils.replaceUUIDsWithMCIDs
import java.awt.Color

object DiscordMessageHandler {
    fun handleDiscordMessage(event: MessageReceivedEvent, server: MinecraftServer) {
        Fabricord.executorService.submit {
            val message: Text = createMessage(event, false, null, server.registryManager) ?: return@submit
            sendToAllPlayers(server, message)
        }
    }

    fun handleMentionedDiscordMessage(event: MessageReceivedEvent, server: MinecraftServer, mentionedPlayers: List<ServerPlayerEntity>, foundUUID: Boolean) {
        Fabricord.executorService.submit {
            val updatedMessageContent = replaceUUIDsWithMCIDs(event.message.contentRaw, server.playerManager.playerList)

            mentionedPlayers.forEach { player ->
                player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_PLING.comp_349(), SoundCategory.MASTER, 2.0f, 2.0f)
            }

            val mentionMessage: Text =
                createMessage(event, true, if (foundUUID) updatedMessageContent.first else event.message.contentRaw, server.registryManager) ?: return@submit
            val generalMessage: Text =
                createMessage(event, false, if (foundUUID) updatedMessageContent.first else event.message.contentRaw, server.registryManager) ?: return@submit

            mentionedPlayers.forEach { player ->
                player.sendMessage(mentionMessage, false)
            }

            val nonMentionedPlayers = if (foundUUID) updatedMessageContent.second else mentionedPlayers
            sendToAllPlayersExcept(server, generalMessage, nonMentionedPlayers)
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

    private fun createMessage(event: MessageReceivedEvent, isMention: Boolean, updatedContent: String?, rm: DynamicRegistryManager.Immutable): Text? {
        val channelId: String = Fabricord.logChannelID!!
        if (event.channel.id != channelId || event.author.isBot) {
            return null
        }

        val member = event.member
        val memberName = member?.user?.globalName ?: "Unknown Name"
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
        return Text.Serialization.fromJson(json, rm)
    }
}
