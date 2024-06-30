package net.rk4z.fabricord.events

import net.rk4z.beacon.Event

class ServerStartEvent : Event() {
    companion object {
        fun get() = ServerStartEvent()
    }
}

class ServerStopEvent : Event() {
    companion object {
        fun get() = ServerStopEvent()
    }
}