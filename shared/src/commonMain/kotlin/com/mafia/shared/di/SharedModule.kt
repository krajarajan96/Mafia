package com.mafia.shared.di

import com.mafia.shared.game.GameEngine
import com.mafia.shared.network.GameSocket
import com.mafia.shared.repository.GameRepository
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import org.koin.dsl.module

val sharedModule = module {
    single { HttpClient { install(WebSockets) } }
    single { GameSocket(get()) }
    single { GameEngine() }
    single { GameRepository(get(), get()) }
}
