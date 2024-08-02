package net.rk4z.fabricord.discord

import net.rk4z.fabricord.Fabricord
import net.dv8tion.jda.api.EmbedBuilder
import net.minecraft.server.network.ServerPlayerEntity
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
        sendEmbedToDiscord(Color.GREEN, "$name joined the server", imageUrl)
    }

    fun sendPlayerLeftEmbed(player: ServerPlayerEntity) {
        val name = player.name.string
        val uuid = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        sendEmbedToDiscord(Color.RED, "$name left the server", imageUrl)
    }

    fun sendPlayerDeathEmbed(player: ServerPlayerEntity, deathMessage: String) {
        val uuid = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        sendEmbedToDiscord(Color.BLACK, deathMessage, imageUrl)
    }

    fun sendPlayerGrantCriterionEmbed(player: ServerPlayerEntity, criterion: String) {
        val name = player.name.string
        val uuid = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        sendEmbedToDiscord(Color.YELLOW, "$name has made the advancement $criterion", imageUrl)
    }
}