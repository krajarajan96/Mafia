package com.mafia.server.game

import com.mafia.shared.game.GameEngine
import com.mafia.shared.model.*
import com.mafia.shared.network.messages.ServerMessage
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class GameSession(
    val roomId: String, val roomCode: String, var mode: GameMode,
    private val aiController: AIPlayerController? = null
) {
    private val engine = GameEngine()
    private val connections = ConcurrentHashMap<String, WebSocketSession>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    var state = GameState(roomId = roomId); private set
    var room = Room(id = roomId, code = roomCode, hostId = "", mode = mode); private set
    private var phaseTimerJob: Job? = null
    private val resolveMutex = Mutex()
    private var gameStarted = false

    fun addPlayer(player: Player, session: WebSocketSession): Boolean {
        if (room.isFull || state.phase != GamePhase.LOBBY) return false
        connections[player.id] = session
        state = state.copy(players = state.players + player)
        room = room.copy(players = state.players.map { PlayerPublicInfo.from(it) }, hostId = if (room.hostId.isEmpty()) player.id else room.hostId)
        return true
    }

    suspend fun removePlayer(playerId: String) {
        connections.remove(playerId)
        state = state.copy(players = state.players.filter { it.id != playerId })
        room = room.copy(players = state.players.map { PlayerPublicInfo.from(it) })
        broadcastAll(ServerMessage.PlayerLeft(playerId))
    }

    fun getConnection(playerId: String): WebSocketSession? = connections[playerId]

    suspend fun startGame() {
        if (gameStarted) return
        gameStarted = true
        if (room.settings.allowAIFill && state.players.size < room.minPlayers) {
            val needed = room.minPlayers - state.players.size
            state = state.copy(players = state.players + AI_PERSONALITIES.shuffled().take(needed))
            room = room.copy(players = state.players.map { PlayerPublicInfo.from(it) })
        }
        state = state.copy(players = engine.assignRoles(state.players, room.settings))
        room = room.copy(status = RoomStatus.IN_GAME)
        broadcastAll(ServerMessage.GameStarted(state.players.size))
        state.players.forEach { p -> p.role?.let { sendTo(p.id, ServerMessage.RoleAssigned(it)) } }
        transitionTo(GamePhase.ROLE_REVEAL)
    }

    private suspend fun transitionTo(phase: GamePhase) {
        phaseTimerJob?.cancel()
        state = when (phase) {
            GamePhase.NIGHT -> state.copy(phase = phase, round = state.round + 1, nightActions = NightActions(), votes = emptyMap(), skips = emptySet(), eliminatedThisRound = null)
            GamePhase.DISCUSSION -> state.copy(phase = phase, votes = emptyMap(), skips = emptySet())
            GamePhase.VOTING -> state.copy(phase = phase, votes = emptyMap(), skips = emptySet())
            else -> state.copy(phase = phase)
        }
        val duration = when (phase) {
            GamePhase.NIGHT -> room.settings.nightTimeSec; GamePhase.DISCUSSION -> room.settings.discussionTimeSec
            GamePhase.VOTING -> room.settings.votingTimeSec; else -> phase.defaultDurationSeconds
        }
        broadcastAll(ServerMessage.PhaseChanged(phase, state.round, duration, state.alivePlayers.map { PlayerPublicInfo.from(it) }))
        if (phase == GamePhase.NIGHT || phase == GamePhase.DISCUSSION || phase == GamePhase.VOTING)
            scope.launch { triggerAIActions(phase) }
        if (duration > 0) startPhaseTimer(duration, phase)
    }

    private fun startPhaseTimer(seconds: Int, phase: GamePhase) {
        phaseTimerJob = scope.launch {
            var remaining = seconds
            while (remaining > 0) { delay(1000); remaining--; broadcastAll(ServerMessage.TimerTick(remaining)) }
            onPhaseTimeout(phase)
        }
    }

    private suspend fun onPhaseTimeout(phase: GamePhase) {
        when (phase) {
            GamePhase.ROLE_REVEAL -> transitionTo(GamePhase.NIGHT)
            GamePhase.NIGHT -> resolveNight()
            GamePhase.NIGHT_RESULT -> transitionTo(GamePhase.DISCUSSION)
            GamePhase.DISCUSSION -> transitionTo(GamePhase.VOTING)
            GamePhase.VOTING -> resolveVoting()
            GamePhase.ELIMINATION -> { val w = state.checkWinCondition(); if (w != null) endGame(w) else transitionTo(GamePhase.NIGHT) }
            else -> {}
        }
    }

    suspend fun submitNightAction(playerId: String, targetId: String) {
        state = engine.submitNightAction(state, playerId, targetId)
        sendTo(playerId, ServerMessage.NightActionAck(true))
        val player = state.getPlayer(playerId)
        if (player?.role == Role.DETECTIVE) {
            state.getPlayer(targetId)?.let {
                sendTo(playerId, ServerMessage.DetectiveResult(targetId, it.name, it.role?.isMafia() == true))
            }
        }
        if (engine.allNightActionsSubmitted(state)) {
            resolveMutex.withLock {
                if (state.phase == GamePhase.NIGHT) { phaseTimerJob?.cancel(); resolveNight() }
            }
        }
    }

    suspend fun submitMinisterVeto(playerId: String) {
        state = engine.submitMinisterVeto(state, playerId)
        if (engine.allVotedOrSkipped(state)) {
            resolveMutex.withLock {
                if (state.phase == GamePhase.VOTING) { phaseTimerJob?.cancel(); resolveVoting() }
            }
        }
    }

    fun updateSettings(settings: GameSettings) {
        room = room.copy(settings = settings)
        broadcastAll(ServerMessage.SettingsUpdated(settings))
    }

    private suspend fun resolveNight() {
        val prevState = state
        state = engine.processNightActions(state)
        val eliminated = state.eliminatedThisRound?.let { state.getPlayer(it) }
        val vigilanteKilled = prevState.nightActions.resolve().vigilanteTargetId?.let { state.getPlayer(it) }
        val vigilanteElim = state.vigilanteEliminated?.let { state.getPlayer(it) }
        broadcastAll(ServerMessage.NightSummary(
            eliminated?.let { PlayerPublicInfo.from(it) },
            if (room.settings.revealRoleOnDeath) eliminated?.role else null,
            state.savedThisNight,
            vigilanteKilled?.let { PlayerPublicInfo.from(it) },
            vigilanteElim?.let { PlayerPublicInfo.from(it) }
        ))
        if (state.winner != null) endGame(state.winner!!) else transitionTo(GamePhase.NIGHT_RESULT)
    }

    suspend fun submitVote(voterId: String, targetId: String) {
        state = engine.submitVote(state, voterId, targetId)
        val tally = state.votes.values.groupingBy { it }.eachCount()
        broadcastAll(ServerMessage.VoteUpdate(voterId, targetId, tally, state.skips))
        if (engine.allVotedOrSkipped(state)) {
            resolveMutex.withLock {
                if (state.phase == GamePhase.VOTING) { phaseTimerJob?.cancel(); resolveVoting() }
            }
        }
    }

    suspend fun submitSkip(voterId: String) {
        state = engine.submitSkip(state, voterId)
        val tally = state.votes.values.groupingBy { it }.eachCount()
        broadcastAll(ServerMessage.VoteUpdate(voterId, "SKIP", tally, state.skips))
        if (engine.allVotedOrSkipped(state)) {
            resolveMutex.withLock {
                if (state.phase == GamePhase.VOTING) { phaseTimerJob?.cancel(); resolveVoting() }
            }
        }
    }

    private suspend fun resolveVoting() {
        val eliminatedId = state.voteResult()
        state = engine.processVote(state)
        val eliminated = eliminatedId?.let { state.getPlayer(it) }
        val tally = state.votes.values.groupingBy { it }.eachCount()
        broadcastAll(ServerMessage.VoteResult(
            eliminated?.let { PlayerPublicInfo.from(it) },
            if (room.settings.revealRoleOnDeath) eliminated?.role else null,
            eliminatedId == null, tally
        ))
        if (state.winner != null) endGame(state.winner!!) else transitionTo(GamePhase.ELIMINATION)
    }

    suspend fun handleChat(senderId: String, text: String) {
        val sender = state.getPlayer(senderId) ?: return
        if (!sender.isAlive && state.phase.isDayPhase()) return
        val message = ChatMessage("${roomId}_${System.currentTimeMillis()}", senderId, sender.name, text, System.currentTimeMillis())
        state = state.copy(chatMessages = state.chatMessages + message)
        broadcastAll(ServerMessage.ChatReceived(message))
    }

    private suspend fun triggerAIActions(phase: GamePhase) {
        state.alivePlayers.filter { it.isAI }.forEach { player ->
            scope.launch {
                delay((1000L..3000L).random())
                aiController?.let { ai ->
                    when (phase) {
                        GamePhase.NIGHT -> ai.chooseNightTarget(state, player)?.let { submitNightAction(player.id, it) }
                        GamePhase.DISCUSSION -> ai.generateDiscussion(state, player).forEach { msg -> delay((2000L..5000L).random()); handleChat(player.id, msg) }
                        GamePhase.VOTING -> ai.chooseVoteTarget(state, player)?.let { submitVote(player.id, it) }
                        else -> {}
                    }
                }
            }
        }
    }

    private suspend fun endGame(winner: Team) {
        phaseTimerJob?.cancel()
        state = state.copy(phase = GamePhase.GAME_OVER, winner = winner)
        room = room.copy(status = RoomStatus.FINISHED)
        broadcastAll(ServerMessage.GameOver(winner, state.players.associate { it.id to (it.role ?: Role.TOWNSFOLK) }))
    }

    private fun broadcastAll(message: ServerMessage) {
        val encoded = ServerMessage.encode(message)
        connections.forEach { (_, s) -> scope.launch { try { s.send(Frame.Text(encoded)) } catch (_: Exception) {} } }
    }

    private fun sendTo(playerId: String, message: ServerMessage) {
        connections[playerId]?.let { s -> scope.launch { try { s.send(Frame.Text(ServerMessage.encode(message))) } catch (_: Exception) {} } }
    }
}
