package com.mafia.server.routes

import com.mafia.server.game.GameSessionManager
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.configureRoomRoutes(manager: GameSessionManager) {
    get("/health") { call.respond(mapOf("status" to "ok", "rooms" to manager.activeRoomCount())) }
    get("/rooms") { call.respond(manager.listOpenRooms()) }
}
