package net.elysium.mod.fabricord.discord;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.elysium.mod.fabricord.ConfigManager;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

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

        // Check if the message is from the specified channel
        if (!event.getChannel().getId().equals(channelId)) return;

        if (event.getAuthor().isBot()) return;

        var member = event.getMember();
        String memberName = member != null ? member.getUser().getName() : "Unknown Name";

        Text textMessage = Text.of(("[Discord" + memberName + "]" + " » " + event.getMessage().getContentDisplay()));

        // Sending the message to all players on the server
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(textMessage, false);
        }
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
        ServerMessageEvents.CHAT_MESSAGE.register((player, message, viewer) -> {
            String chatMessage = String.format("%s » %s", message.getEntityName(), message);
            sendToDiscord(chatMessage);
        });

    }
}
