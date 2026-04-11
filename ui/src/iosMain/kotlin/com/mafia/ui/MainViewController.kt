package com.mafia.ui

import androidx.compose.ui.window.ComposeUIViewController
import com.mafia.shared.network.GameSocket
import com.mafia.shared.repository.GameRepository
import com.mafia.ui.navigation.MafiaApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val holder = AppHolder()
    CoroutineScope(Dispatchers.Default).launch { try { holder.socket.connect() } catch (_: Exception) {} }
    return ComposeUIViewController { MafiaApp(repository = holder.repository) }
}

private class AppHolder : KoinComponent {
    val socket: GameSocket by inject()
    val repository: GameRepository by inject()
}
