package net.rk4z.fabricord

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.rk4z.beacon.EventBus
import net.rk4z.fabricord.events.ServerStartEvent
import net.rk4z.fabricord.events.ServerStopEvent

class Initializer : ModInitializer {
    override fun onInitialize() {
        EventBus.initialize()
        EventBus.registerAllListeners("net.rk4z.fabricord")
        EventBus.callEventAsync(ServerStartEvent.get())
        ServerLifecycleEvents.SERVER_STARTING.register {
            EventBus.callEventAsync(ServerStopEvent.get())
        }
    }
}
