package net.elysium.mod.fabricord.discord;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.elysium.mod.fabricord.ConfigManager;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Comparator;

public class DiscordBot {

    private final ListenerAdapter discordListener = new ListenerAdapter() {
        @Override
        public void onMessageReceived(@NotNull MessageReceivedEvent event) {
           handleDiscordMessage(event);
        }
    };

    private final MinecraftServer server;

    public JDA jda;

    public DiscordBot(MinecraftServer server) {
        this.server = server;
    }

    public void startBot() {
        String token = ConfigManager.getBotToken();
        if (token == null || token.isEmpty()) {
            return;
        }

        try {
            JDABuilder builder = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT);

            OnlineStatus status = switch (ConfigManager.getBotOnlineStatus()) {
                case "IDLE" -> OnlineStatus.IDLE;
                case "DO_NOT_DISTURB", "DND" -> OnlineStatus.DO_NOT_DISTURB;
                default -> OnlineStatus.ONLINE;
            };
            builder.setStatus(status);

            Activity activity = Activity.playing("Minecraft");
            builder.setActivity(activity);

            jda = builder.build();
            jda.awaitReady();
            jda.addEventListener(discordListener);

            sendToDiscord("**:white_check_mark: Server has Started!**");
        } catch (Exception e) {
            // Handle exception
        }
    }

    public void stopBot() {
        sendToDiscord("**:octagonal_sign: Server has Stopped!**");
        if (jda != null) {
            jda.shutdown();
            jda = null;
        }
    }

    private void sendToDiscord(String message) {
        String channelId = ConfigManager.getLogChannelID();
        if (channelId == null || channelId.isEmpty()) {
            // Handle missing channel ID
            return;
        }
        jda.getTextChannelById(channelId).sendMessage(message).queue();
    }

    public void handleDiscordMessage(MessageReceivedEvent event) {
        String channelId = ConfigManager.getLogChannelID();
        if (!event.getChannel().getId().equals(channelId) || event.getAuthor().isBot()) {
            return;
        }

        var member = event.getMember();
        var memberName = member != null ? member.getUser().getName() : "Unknown Name";
        var memberId = member != null ? member.getUser().getId() : "00000000000000000000";
        var idSuggest = "<@" + memberId + ">";
        var highestRole = member != null ? member.getRoles().stream().max(Comparator.comparingInt(Role::getPosition)).orElse(null) : null;
        var roleName = highestRole != null ? highestRole.getName() : "Unknown";
        var roleColor = highestRole != null && highestRole.getColor() != null ? highestRole.getColor() : Color.WHITE;
        var kyoriRoleColor = TextColor.color(roleColor.getRed(), roleColor.getGreen(), roleColor.getBlue());

        var componentMessage = Component.text("[", TextColor.color(0xFFFFFF))
                .append(Component.text("Discord", TextColor.color(0x55CDFC)))
                .append(Component.text(" | ", TextColor.color(0xFFFFFF)))
                .append(Component.text(roleName, kyoriRoleColor))
                .append(Component.text("]", TextColor.color(0xFFFFFF)))
                .append(Component.text(" "))
                .append(Component.text(memberName)
                        .clickEvent(ClickEvent.suggestCommand(idSuggest)))
                .append(Component.text(" » " + event.getMessage().getContentDisplay()));

        String json = GsonComponentSerializer.gson().serialize(componentMessage);

        net.minecraft.text.Text textMessage = net.minecraft.text.Text.Serializer.fromJson(json);

        server.getPlayerManager().getPlayerList().forEach(player ->
                player.sendMessage(textMessage, false)
        );
    }

    public void registerEventListeners() {
        // プレイヤーがサーバーに参加したときのイベントハンドラー
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            DiscordEmbed.sendPlayerJoinEmbed(player, this);
        });

        // プレイヤーがサーバーから退出したときのイベントハンドラー
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            DiscordEmbed.sendPlayerLeftEmbed(player, this);
        });

        // プレイヤーがチャットメッセージを送信したときのイベントハンドラー
        ServerMessageEvents.CHAT_MESSAGE.register((Chatmessage, sender, parameters) -> {
            String user = sender.getDisplayName().getString();
            String message = Chatmessage.getContent().getString();
            String chatMessage = String.format("%s » %s", user, message);
            sendToDiscord(chatMessage);
        });

    }
}
