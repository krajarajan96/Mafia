package com.mafia.shared.di

import com.mafia.shared.game.GameEngine
import com.mafia.shared.network.GameSocket
import com.mafia.shared.repository.GameRepository
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import org.koin.dsl.module

/**
 * The production server URL. After deploying to Railway, update this to your assigned domain:
 *   e.g. "wss://mafia-server-production.up.railway.app"
 * Use "wss://" for Railway (HTTPS/WSS) and "ws://" only for local development.
 */
const val SERVER_BASE_URL = "wss://mafia-server-production.up.railway.app"

fun sharedModule(serverBaseUrl: String = SERVER_BASE_URL) = module {
    single { HttpClient { install(WebSockets) } }
    single { GameSocket(get(), serverBaseUrl) }
    single { GameEngine() }
    single { GameRepository(get(), get()) }
}
