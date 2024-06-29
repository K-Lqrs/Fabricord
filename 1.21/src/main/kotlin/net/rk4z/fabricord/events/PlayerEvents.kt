package net.rk4z.fabricord.events

import net.minecraft.server.network.ServerPlayerEntity
import net.rk4z.beacon.Event

class PlayerJoinEvent(val player: ServerPlayerEntity): Event() {
    companion object {
        @JvmStatic
        fun get(player: ServerPlayerEntity): PlayerJoinEvent {
            return PlayerJoinEvent(player)
        }
    }
}

class PlayerLeaveEvent(val player: ServerPlayerEntity) : Event() {
    companion object {
        @JvmStatic
        fun get(player: ServerPlayerEntity): PlayerLeaveEvent {
            return PlayerLeaveEvent(player)
        }
    }
}

class PlayerDeathEvent(val player: ServerPlayerEntity) : Event() {
    companion object {
        @JvmStatic
        fun get(player: ServerPlayerEntity): PlayerDeathEvent {
            return PlayerDeathEvent(player)
        }
    }
}

class PlayerGrantCriterionEvent(val player: ServerPlayerEntity, val criterion: String) : Event() {
    companion object {
        @JvmStatic
        fun get(player: ServerPlayerEntity, criterion: String): PlayerGrantCriterionEvent {
            return PlayerGrantCriterionEvent(player, criterion)
        }
    }
}