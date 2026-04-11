package com.mafia.server

import com.mafia.server.game.GameSessionManager
import com.mafia.server.routes.configureGameWebSocket
import com.mafia.server.routes.configureRoomRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import java.time.Duration

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json { prettyPrint = false; ignoreUnknownKeys = true; classDiscriminator = "type" })
    }
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(30)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    val sessionManager = GameSessionManager()
    routing {
        configureRoomRoutes(sessionManager)
        configureGameWebSocket(sessionManager)
    }
}
