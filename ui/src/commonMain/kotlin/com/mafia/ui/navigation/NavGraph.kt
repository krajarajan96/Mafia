package com.mafia.ui.navigation

import androidx.compose.runtime.*
import com.mafia.shared.model.*
import com.mafia.shared.network.messages.ServerMessage
import com.mafia.shared.repository.GameRepository
import com.mafia.ui.screens.*
import com.mafia.ui.theme.MafiaTheme
import kotlinx.coroutines.launch

sealed class Screen {
    data object Home : Screen()
    data class Lobby(val isMultiplayer: Boolean) : Screen()
    data object WaitingRoom : Screen()
    data object Game : Screen()
    data object HowToPlay : Screen()
}

@Composable
fun MafiaApp(repository: GameRepository) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val scope = rememberCoroutineScope()
    val room by repository.room.collectAsState()
    val myPlayerId by repository.myPlayerId.collectAsState()
    val myRole by repository.myRole.collectAsState()
    val phase by repository.phase.collectAsState()
    val round by repository.round.collectAsState()
    val alivePlayers by repository.alivePlayers.collectAsState()
    val chatMessages by repository.chatMessages.collectAsState()
    val timer by repository.timer.collectAsState()
    val voteTally by repository.voteTally.collectAsState()
    val detectiveResult by repository.detectiveResult.collectAsState()
    val lastEvent by repository.lastEvent.collectAsState(initial = null)

    LaunchedEffect(Unit) {
        repository.lastEvent.collect { event ->
            when (event) {
                is ServerMessage.RoomCreated, is ServerMessage.RoomJoined -> currentScreen = Screen.WaitingRoom
                is ServerMessage.GameStarted -> currentScreen = Screen.Game
                else -> {}
            }
        }
    }

    MafiaTheme(darkTheme = true) {
        when (val screen = currentScreen) {
            is Screen.Home -> HomeScreen(
                onSinglePlayer = { currentScreen = Screen.Lobby(false) },
                onMultiplayer = { currentScreen = Screen.Lobby(true) },
                onHowToPlay = { currentScreen = Screen.HowToPlay }
            )
            is Screen.Lobby -> LobbyScreen(screen.isMultiplayer,
                onCreateRoom = { n, e, m -> scope.launch { repository.createRoom(m, n, e) } },
                onJoinRoom = { c, n, e -> scope.launch { repository.joinRoom(c, n, e) } },
                onBack = { currentScreen = Screen.Home })
            is Screen.WaitingRoom -> room?.let { r -> myPlayerId?.let { pid ->
                WaitingRoomScreen(r, pid, onStartGame = { repository.startGame() }, onLeave = { repository.leaveRoom(); currentScreen = Screen.Home })
            } }
            is Screen.Game -> GameScreen(phase, round, myRole, myPlayerId, alivePlayers, chatMessages, timer, voteTally, detectiveResult, lastEvent,
                onNightAction = { repository.submitNightAction(it) }, onSendChat = { repository.sendChat(it) }, onVote = { repository.castVote(it) },
                onLeave = { repository.leaveRoom(); repository.resetForNewGame(); currentScreen = Screen.Home })
            is Screen.HowToPlay -> HowToPlayScreen(onBack = { currentScreen = Screen.Home })
        }
    }
}
