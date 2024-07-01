package net.rk4z.fabricord.events

import net.rk4z.beacon.Event

class ServerLoadEvent : Event() {
    companion object {
        @JvmStatic
        fun get() = ServerLoadEvent()
    }
}

class ServerStartEvent : Event() {
    companion object {
        @JvmStatic
        fun get() = ServerStartEvent()
    }
}

class ServerStopEvent : Event() {
    companion object {
        @JvmStatic
        fun get() = ServerStopEvent()
    }
}