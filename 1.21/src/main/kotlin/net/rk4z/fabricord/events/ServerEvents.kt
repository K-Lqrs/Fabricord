package net.rk4z.fabricord.events

import net.rk4z.beacon.Event

class ServerStartEvent: Event() {
    companion object {
        private val instance = ServerStartEvent()
        @JvmStatic
        fun get(): ServerStartEvent {
            return instance
        }
    }
}
