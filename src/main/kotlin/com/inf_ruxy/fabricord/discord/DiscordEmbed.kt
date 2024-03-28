package com.inf_ruxy.fabricord.discord

import com.inf_ruxy.fabricord.Fabricord.Companion.logger
import com.inf_ruxy.fabricord.FabricordApi.discordBotManager
import com.inf_ruxy.fabricord.util.ConfigManager.Companion.logChannelID
import net.dv8tion.jda.api.EmbedBuilder
import net.minecraft.advancement.Advancement
import net.minecraft.server.network.ServerPlayerEntity
import java.awt.Color
import java.util.*


class DiscordEmbed {

    private fun sendEmbedToDiscord(color: Color, author: String? = null, imageUrl: String, channelId: String = logChannelID!!) {
        if (channelId.isBlank()) {
            logger.error("Channel ID is blank.")
            return
        }

        val embed = EmbedBuilder().apply {
            setColor(color)
            setAuthor(author, null, imageUrl)
        }.build()

        discordBotManager.jda?.getTextChannelById(channelId)?.sendMessageEmbeds(embed)?.queue()
    }

    fun sendPlayerJoinEmbed(player: ServerPlayerEntity) {
        val uuid = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        sendEmbedToDiscord(Color.GREEN, "${player.entityName} joined the server", imageUrl)
    }

    fun sendPlayerLeftEmbed(player: ServerPlayerEntity) {
        val uuid = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        sendEmbedToDiscord(Color.RED, "${player.entityName} left the server", imageUrl)
    }

    fun sendPlayerDeathEmbed(player: String, uuid: UUID, deathMessage: String?) {
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        sendEmbedToDiscord(Color.BLACK, deathMessage ?: "$player died", imageUrl)
    }

    fun sendAdvancementEmbed(player: ServerPlayerEntity, advancement: Advancement) {

        val advancementTitle = advancement.display?.title?.string?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                Locale.getDefault()
            ) else it.toString()
        }
            ?: advancement.id.path.split("/").last().replace('_', ' ')
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        val message = "${player.entityName} has made the advancement $advancementTitle"
        val imageUrl = "https://visage.surgeplay.com/face/256/${player.uuid}"

        sendEmbedToDiscord(
            Color(128, 0, 128),
            "$message",
            imageUrl,

        )
    }


}
