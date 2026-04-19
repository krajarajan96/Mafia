package com.mafia.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mafia.shared.model.*
import com.mafia.shared.network.GameSocket
import com.mafia.shared.network.messages.ServerMessage
import com.mafia.shared.repository.GameRepository
import com.mafia.ui.BackHandlerEffect
import com.mafia.ui.screens.*
import com.mafia.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen {
    data object Home : Screen()
    data object Lobby : Screen()
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
    val nightSummary by repository.nightSummary.collectAsState()
    val voteResult by repository.voteResult.collectAsState()
    val voteLog by repository.voteLog.collectAsState()
    val eventLog by repository.eventLog.collectAsState()
    val ministerVetoUsed by repository.ministerVetoUsed.collectAsState()
    val revealedRoles by repository.revealedRoles.collectAsState()
    val allPlayers by repository.allPlayers.collectAsState()
    val lastEvent by repository.lastEvent.collectAsState(initial = null)
    val mafiaTeammates by repository.mafiaTeammates.collectAsState()
    val mafiaChatMessages by repository.mafiaChatMessages.collectAsState()
    val mafiaVoteTally by repository.mafiaVoteTally.collectAsState()
    val mafiaVoteTie by repository.mafiaVoteTie.collectAsState()
    val enableGameHistory = room?.settings?.enableGameHistory ?: true
    val enableTips = room?.settings?.enableTips ?: true
    val spectators by repository.spectators.collectAsState()
    val rematchInitiated by repository.rematchInitiated.collectAsState()
    val rematchReadyIds by repository.rematchReadyIds.collectAsState()
    val rematchTotalPlayers by repository.rematchTotalPlayers.collectAsState()
    val errorMessage by repository.errorMessage.collectAsState()
    val connectionState by repository.connectionState.collectAsState()

    // Auto-clear error after 4 seconds
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) { delay(4000); repository.clearError() }
    }

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
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            when (currentScreen) {

                is Screen.Home -> HomeScreen(
                    onPlay = { currentScreen = Screen.Lobby },
                    onHowToPlay = { currentScreen = Screen.HowToPlay }
                )
                is Screen.Lobby -> {
                    BackHandlerEffect { currentScreen = Screen.Home }
                    LobbyScreen(
                        onCreateRoom = { n, e -> scope.launch { repository.createRoom(GameMode.MULTIPLAYER, n, e) } },
                        onJoinRoom = { c, n, e -> scope.launch { repository.joinRoom(c, n, e) } },
                        onJoinAsSpectator = { c, n, e -> scope.launch { repository.joinAsSpectator(c, n, e) } },
                        onBack = { currentScreen = Screen.Home }
                    )
                }
                is Screen.WaitingRoom -> {
                    BackHandlerEffect { repository.leaveRoom(); currentScreen = Screen.Home }
                    room?.let { r -> myPlayerId?.let { pid ->
                        WaitingRoomScreen(
                            r, pid,
                            onStartGame = { repository.startGame() },
                            onLeave = { repository.leaveRoom(); currentScreen = Screen.Home },
                            onUpdateSettings = { repository.updateSettings(it) }
                        )
                    } }
                }
                is Screen.Game -> {
                    BackHandlerEffect {}
                    GameScreen(
                        phase, round, myRole, myPlayerId, alivePlayers, chatMessages, timer,
                        voteTally, detectiveResult, nightSummary, voteResult, voteLog,
                        eventLog, ministerVetoUsed, revealedRoles, allPlayers, lastEvent,
                        mafiaTeammates, mafiaChatMessages, mafiaVoteTally, mafiaVoteTie,
                        enableGameHistory, enableTips, spectators, rematchInitiated, rematchReadyIds, rematchTotalPlayers,
                        onNightAction = { repository.submitNightAction(it) },
                        onSendChat = { repository.sendChat(it) },
                        onSendMafiaChat = { repository.sendMafiaChat(it) },
                        onVote = { repository.castVote(it) },
                        onSkipVote = { repository.skipVote() },
                        onUseVeto = { repository.useMinisterVeto() },
                        onInitiateRematch = { repository.initiateRematch() },
                        onMarkRematchReady = { repository.markRematchReady() },
                        onLeave = { repository.leaveRoom(); repository.resetForNewGame(); currentScreen = Screen.Home }
                    )
                }
                is Screen.HowToPlay -> {
                    BackHandlerEffect { currentScreen = Screen.Home }
                    HowToPlayScreen(onBack = { currentScreen = Screen.Home })
                }
            }

            // Connection banner — shown when not on Home/Lobby and connection is lost
            val showConnectionBanner = connectionState != GameSocket.ConnectionState.CONNECTED &&
                currentScreen !is Screen.Home && currentScreen !is Screen.Lobby
            AnimatedVisibility(
                visible = showConnectionBanner,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
            ) {
                Surface(color = Color(0xFF2A1500)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (connectionState == GameSocket.ConnectionState.RECONNECTING) {
                            CircularProgressIndicator(Modifier.size(13.dp), strokeWidth = 2.dp, color = MafiaGold)
                            Spacer(Modifier.width(8.dp))
                            Text("Reconnecting...", fontSize = 13.sp, color = MafiaGold)
                        } else {
                            Text("⚡ Connection lost", fontSize = 13.sp, color = MafiaRed.copy(0.9f))
                        }
                    }
                }
            }

            // Error banner — shown for server errors (room not found, room full, etc.)
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                Surface(
                    color = MafiaRed.copy(0.93f),
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️", fontSize = 16.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            errorMessage ?: "",
                            fontSize = 14.sp, color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { repository.clearError() },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("✕", color = Color.White.copy(0.7f), fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
