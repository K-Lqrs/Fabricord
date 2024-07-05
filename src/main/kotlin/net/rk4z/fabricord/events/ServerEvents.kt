package net.rk4z.fabricord.events

import net.minecraft.server.MinecraftServer
import net.rk4z.beacon.Event

class ServerInitEvent(
    val s: MinecraftServer
) : Event() {
    companion object {
        @JvmStatic
        fun get(s: MinecraftServer) = ServerInitEvent(s)
    }
}

class ServerStartEvent : Event() {
    companion object {
        @JvmStatic
        fun get() = ServerStartEvent()
    }
}

class ServerShutdownEvent : Event() {
    companion object {
        @JvmStatic
        fun get() = ServerShutdownEvent()
    }
}