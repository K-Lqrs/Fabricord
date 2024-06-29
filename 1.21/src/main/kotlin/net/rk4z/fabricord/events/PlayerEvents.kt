package net.rk4z.fabricord.events

import net.rk4z.beacon.Event

class PlayerJoinEvent: Event() {
    companion object {
        private val instance = PlayerJoinEvent()
        @JvmStatic
        fun get(): PlayerJoinEvent {
            return instance
        }
    }
}

class PlayerLeaveEvent: Event() {
    companion object {
        private val instance = PlayerLeaveEvent()
        @JvmStatic
        fun get(): PlayerLeaveEvent {
            return instance
        }
    }
}