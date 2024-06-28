package net.rk4z.fabricord

import net.rk4z.beacon.Listener
import net.rk4z.beacon.handler
import net.rk4z.fabricord.events.ServerStartEvent

class Fabricord : Listener {

    val startServer = handler<ServerStartEvent> {

    }
}