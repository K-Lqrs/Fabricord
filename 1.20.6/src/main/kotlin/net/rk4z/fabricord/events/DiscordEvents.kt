package net.rk4z.fabricord.events

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.rk4z.beacon.Event

class DiscordMessageReceiveEvent(val c: MessageReceivedEvent, val s: MinecraftServer) : Event() {
    companion object {
        @JvmStatic
        fun get(event: MessageReceivedEvent, server: MinecraftServer): DiscordMessageReceiveEvent {
            return DiscordMessageReceiveEvent(event, server)
        }
    }
}

class DiscordMCPlayerMentionEvent(
    val c: MessageReceivedEvent,
    val s: MinecraftServer,
    val m: List<ServerPlayerEntity>,
    val fu: Boolean
) : Event() {
    companion object {
        @JvmStatic
        fun get(
            event: MessageReceivedEvent,
            server: MinecraftServer,
            mentionedPlayers: List<ServerPlayerEntity>,
            foundUUID: Boolean
        ): DiscordMCPlayerMentionEvent {
            return DiscordMCPlayerMentionEvent(event, server, mentionedPlayers, foundUUID)
        }
    }
}