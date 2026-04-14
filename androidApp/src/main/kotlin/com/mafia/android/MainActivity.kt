package com.mafia.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mafia.shared.di.sharedModule
import com.mafia.shared.network.GameSocket
import com.mafia.shared.repository.GameRepository
import com.mafia.ui.navigation.MafiaApp
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.startKoin

class MainActivity : ComponentActivity() {
    private val repository: GameRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (GlobalContext.getOrNull() == null) {
            startKoin { androidContext(applicationContext); modules(sharedModule()) }
        }
        org.koin.java.KoinJavaComponent.get<GameSocket>(GameSocket::class.java).connect()
        setContent { MafiaApp(repository = repository) }
    }
}
