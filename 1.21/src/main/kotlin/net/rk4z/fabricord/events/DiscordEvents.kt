package net.rk4z.fabricord.events

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.minecraft.server.MinecraftServer
import net.rk4z.beacon.Event

class DiscordMessageReceiveEvent(val c: MessageReceivedEvent, val server: MinecraftServer) : Event() {
    companion object {
        @JvmStatic
        fun get(event: MessageReceivedEvent, server: MinecraftServer): DiscordMessageReceiveEvent {
            return DiscordMessageReceiveEvent(event, server)
        }
    }
}

class DiscordMinecraftPlayerMentionEvent(val c: MessageReceivedEvent, val server: MinecraftServer) : Event() {
    companion object {
        @JvmStatic
        fun get(event: MessageReceivedEvent, server: MinecraftServer): DiscordMinecraftPlayerMentionEvent {
            return DiscordMinecraftPlayerMentionEvent(event, server)
        }
    }
}