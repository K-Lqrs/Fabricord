package net.elysium.mod.fabricord.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.elysium.mod.fabricord.ConfigManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.awt.Color;

public class DiscordEmbed {

    public static void sendPlayerJoinEmbed(ServerPlayerEntity player, DiscordBot discordBot) {
        String channelId = ConfigManager.getLogChannelID();
        if (channelId == null || channelId.isEmpty()) {
            // Handle missing channel ID
            return;
        }
        String imageUrl = "https://cravatar.eu/helmavatar/" + player.getUuid().toString() + "/128";
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.GREEN)
                .setAuthor(player.getEntityName() + " joined the server", null, imageUrl);
        discordBot.jda.getTextChannelById(channelId).sendMessageEmbeds(embed.build()).queue();
    }

    public static void sendPlayerLeftEmbed(ServerPlayerEntity player, DiscordBot discordBot) {
        String channelId = ConfigManager.getLogChannelID();
        if (channelId == null || channelId.isEmpty()) {
            // Handle missing channel ID
            return;
        }
        String imageUrl = "https://cravatar.eu/helmavatar/" + player.getUuid().toString() + "/128";
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.RED)
                .setAuthor(player.getEntityName() + " left the server", null, imageUrl);
        discordBot.jda.getTextChannelById(channelId).sendMessageEmbeds(embed.build()).queue();
    }
}
