package com.inf_ruxy.projects.mc.plugin.several.fabricord.discord

import com.inf_ruxy.projects.mc.plugin.several.fabricord.FabricordApi.config
import net.dv8tion.jda.api.EmbedBuilder
import net.minecraft.server.network.ServerPlayerEntity
import java.awt.Color


class DiscordEmbed {

    fun sendPlayerJoinEmbed(player: ServerPlayerEntity, discordBot: DiscordBotManager) {
        val channelId: String = config.logChannelID!!
        if (channelId.isEmpty()) {
            return
        }
        val uuid: String = player.uuid.toString()
        val imageUrl  = "https://visage.surgeplay.com/face/256/$uuid"
        val embed: EmbedBuilder = EmbedBuilder()
            .setColor(Color.GREEN)
            .setAuthor(player.entityName + " joined the server", null, imageUrl)
        discordBot.jda?.getTextChannelById(channelId)?.sendMessageEmbeds(embed.build())?.queue()
    }

    fun sendPlayerLeftEmbed(player: ServerPlayerEntity, discordBot: DiscordBotManager) {
        val channelId: String = config.logChannelID!!
        if (channelId.isEmpty()) {
            return
        }
        val uuid: String = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        val embed: EmbedBuilder = EmbedBuilder()
            .setColor(Color.RED)
            .setAuthor(player.entityName + " left the server", null, imageUrl)
        discordBot.jda?.getTextChannelById(channelId)?.sendMessageEmbeds(embed.build())?.queue()
    }

}