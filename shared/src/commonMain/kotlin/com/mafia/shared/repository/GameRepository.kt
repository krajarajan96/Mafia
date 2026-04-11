package com.mafia.shared.repository

import com.mafia.shared.game.GameEngine
import com.mafia.shared.model.*
import com.mafia.shared.network.GameSocket
import com.mafia.shared.network.messages.ClientMessage
import com.mafia.shared.network.messages.ServerMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class GameRepository(
    private val socket: GameSocket,
    private val engine: GameEngine = GameEngine()
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _room = MutableStateFlow<Room?>(null)
    val room: StateFlow<Room?> = _room.asStateFlow()
    private val _myPlayerId = MutableStateFlow<String?>(null)
    val myPlayerId: StateFlow<String?> = _myPlayerId.asStateFlow()
    private val _myRole = MutableStateFlow<Role?>(null)
    val myRole: StateFlow<Role?> = _myRole.asStateFlow()
    private val _phase = MutableStateFlow(GamePhase.LOBBY)
    val phase: StateFlow<GamePhase> = _phase.asStateFlow()
    private val _round = MutableStateFlow(0)
    val round: StateFlow<Int> = _round.asStateFlow()
    private val _alivePlayers = MutableStateFlow<List<PlayerPublicInfo>>(emptyList())
    val alivePlayers: StateFlow<List<PlayerPublicInfo>> = _alivePlayers.asStateFlow()
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()
    private val _timer = MutableStateFlow(0)
    val timer: StateFlow<Int> = _timer.asStateFlow()
    private val _lastEvent = MutableSharedFlow<ServerMessage>(replay = 1)
    val lastEvent: SharedFlow<ServerMessage> = _lastEvent.asSharedFlow()
    private val _detectiveResult = MutableStateFlow<ServerMessage.DetectiveResult?>(null)
    val detectiveResult: StateFlow<ServerMessage.DetectiveResult?> = _detectiveResult.asStateFlow()
    private val _voteTally = MutableStateFlow<Map<String, Int>>(emptyMap())
    val voteTally: StateFlow<Map<String, Int>> = _voteTally.asStateFlow()
    val connectionState = socket.connectionState

    init {
        scope.launch { socket.incoming.collect { handleMessage(it) } }
    }

    private suspend fun handleMessage(msg: ServerMessage) {
        _lastEvent.emit(msg)
        when (msg) {
            is ServerMessage.RoomCreated -> { _room.value = msg.room; _myPlayerId.value = msg.playerId }
            is ServerMessage.RoomJoined -> { _room.value = msg.room; _myPlayerId.value = msg.playerId }
            is ServerMessage.RoomUpdate -> _room.value = msg.room
            is ServerMessage.PlayerJoined -> _room.value = _room.value?.let { it.copy(players = it.players + msg.player) }
            is ServerMessage.PlayerLeft -> _room.value = _room.value?.let { it.copy(players = it.players.filter { p -> p.id != msg.playerId }) }
            is ServerMessage.RoleAssigned -> _myRole.value = msg.role
            is ServerMessage.PhaseChanged -> {
                _phase.value = msg.phase; _round.value = msg.round
                _alivePlayers.value = msg.alivePlayers; _timer.value = msg.durationSeconds
                _voteTally.value = emptyMap()
                if (msg.phase == GamePhase.NIGHT) _detectiveResult.value = null
            }
            is ServerMessage.TimerTick -> _timer.value = msg.secondsRemaining
            is ServerMessage.ChatReceived -> _chatMessages.value = _chatMessages.value + msg.message
            is ServerMessage.DetectiveResult -> _detectiveResult.value = msg
            is ServerMessage.VoteUpdate -> _voteTally.value = msg.currentTally
            is ServerMessage.GameOver -> _phase.value = GamePhase.GAME_OVER
            else -> {}
        }
    }

    fun createRoom(mode: GameMode, name: String, emoji: String = "🕵️") = socket.send(ClientMessage.CreateRoom(mode, name, emoji))
    fun joinRoom(code: String, name: String, emoji: String = "🕵️") = socket.send(ClientMessage.JoinRoom(code, name, emoji))
    fun startGame() = socket.send(ClientMessage.StartGame)
    fun submitNightAction(targetId: String) = socket.send(ClientMessage.NightAction(targetId))
    fun sendChat(text: String) = socket.send(ClientMessage.SendChat(text))
    fun castVote(targetId: String) = socket.send(ClientMessage.CastVote(targetId))
    fun accuse(targetId: String, reason: String) = socket.send(ClientMessage.Accuse(targetId, reason))
    fun leaveRoom() { socket.send(ClientMessage.LeaveRoom); _room.value = null; _myRole.value = null; _phase.value = GamePhase.LOBBY; _chatMessages.value = emptyList() }
    fun resetForNewGame() { _myRole.value = null; _phase.value = GamePhase.LOBBY; _chatMessages.value = emptyList(); _detectiveResult.value = null; _voteTally.value = emptyMap(); _round.value = 0 }
}
