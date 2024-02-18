package com.inf_ruxy.mods.several.fabricord.discord

import com.inf_ruxy.mods.several.fabricord.FabricordApi.config
import com.inf_ruxy.mods.several.fabricord.FabricordApi.discordBotManager
import net.dv8tion.jda.api.EmbedBuilder
import net.minecraft.server.network.ServerPlayerEntity
import java.awt.Color
import java.util.UUID


class DiscordEmbed {

    fun sendPlayerJoinEmbed(player: ServerPlayerEntity) {
        val channelId: String = config.logChannelID!!
        if (channelId.isEmpty()) {
            return
        }
        val uuid: String = player.uuid.toString()
        val imageUrl  = "https://visage.surgeplay.com/face/256/$uuid"
        val embed: EmbedBuilder = EmbedBuilder()
            .setColor(Color.GREEN)
            .setAuthor(player.entityName + " joined the server", null, imageUrl)
        discordBotManager.jda?.getTextChannelById(channelId)?.sendMessageEmbeds(embed.build())?.queue()
    }

    fun sendPlayerLeftEmbed(player: ServerPlayerEntity) {
        val channelId: String = config.logChannelID!!
        if (channelId.isEmpty()) {
            return
        }
        val uuid: String = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        val embed: EmbedBuilder = EmbedBuilder()
            .setColor(Color.RED)
            .setAuthor(player.entityName + " left the server", null, imageUrl)
        discordBotManager.jda?.getTextChannelById(channelId)?.sendMessageEmbeds(embed.build())?.queue()
    }

    fun sendPlayerDeathEmbed(player: String, uuid: UUID, deathMessage: String?) {
        val channelId: String = config.logChannelID!!
        if (channelId.isEmpty()) {
            return
        }
        val suuid = uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$suuid"
        val embed: EmbedBuilder = EmbedBuilder()
            .setColor(Color.BLACK)
            .setAuthor(deathMessage ?: ("$player died"), null, imageUrl)
        discordBotManager.jda?.getTextChannelById(channelId)?.sendMessageEmbeds(embed.build())?.queue()
    }

}