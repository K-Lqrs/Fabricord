package net.rk4z.fabricord.events

import net.rk4z.beacon.Event

class ServerInitEvent : Event() {
    companion object {
        @JvmStatic
        fun get() = ServerInitEvent()
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