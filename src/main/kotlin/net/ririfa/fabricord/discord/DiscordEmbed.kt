package net.ririfa.fabricord.discord

import net.ririfa.fabricord.Fabricord
import net.dv8tion.jda.api.EmbedBuilder
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.ririfa.fabricord.Fabricord.Companion.playerJoinMessage
import net.ririfa.fabricord.Fabricord.Companion.playerLeaveMessage
import java.awt.Color

object DiscordEmbed {
    private fun sendEmbedToDiscord(color: Color, author: String? = null, imageUrl: String, channelId: String = Fabricord.logChannelID!!) {
        if (channelId.isBlank()) {
            Fabricord.logger.error("Channel ID is blank.")
            return
        }

        val embed = EmbedBuilder().apply {
            setColor(color)
            setAuthor(author, null, imageUrl)
        }.build()

        DiscordBotManager.jda?.getTextChannelById(channelId)?.sendMessageEmbeds(embed)?.queue()
    }

    fun sendPlayerJoinEmbed(player: ServerPlayerEntity) {
        val name = player.name.string
        val uuid = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        val message = playerJoinMessage!!.replace("%player%", name)
        sendEmbedToDiscord(Color.GREEN, message, imageUrl)
    }

    fun sendPlayerLeftEmbed(player: ServerPlayerEntity) {
        val name = player.name.string
        val uuid = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        val message = playerLeaveMessage!!.replace("%player%", name)
        sendEmbedToDiscord(Color.RED, message, imageUrl)
    }

    @JvmStatic
    fun sendPlayerDeathEmbed(player: ServerPlayerEntity, deathMessage: Text) {
        val uuid = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        sendEmbedToDiscord(Color.BLACK, deathMessage.string, imageUrl)
    }

    @JvmStatic
    fun sendPlayerGrantCriterionEmbed(player: ServerPlayerEntity, criterion: String) {
        val name = player.name.string
        val uuid = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        sendEmbedToDiscord(Color.YELLOW, "$name has made the advancement $criterion", imageUrl)
    }
}