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
    private val spectatorConnections = ConcurrentHashMap<String, Pair<Player, WebSocketSession>>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    var state = GameState(roomId = roomId); private set
    var room = Room(id = roomId, code = roomCode, hostId = "", mode = mode); private set
    private var phaseTimerJob: Job? = null
    private val resolveMutex = Mutex()
    private var gameStarted = false
    private val rematchReady = mutableSetOf<String>()

    fun addPlayer(player: Player, session: WebSocketSession): Boolean {
        if (room.isFull || state.phase != GamePhase.LOBBY) return false
        connections[player.id] = session
        state = state.copy(players = state.players + player)
        room = room.copy(players = state.players.map { PlayerPublicInfo.from(it) }, hostId = if (room.hostId.isEmpty()) player.id else room.hostId)
        return true
    }

    suspend fun removePlayer(playerId: String) {
        // Check if spectator first
        if (spectatorConnections.containsKey(playerId)) {
            spectatorConnections.remove(playerId)
            room = room.copy(spectators = spectatorConnections.values.map { PlayerPublicInfo.from(it.first) })
            broadcastAll(ServerMessage.SpectatorLeft(playerId))
            return
        }
        val playerName = state.players.find { it.id == playerId }?.name ?: ""
        connections.remove(playerId)
        state = state.copy(players = state.players.filter { it.id != playerId })
        room = room.copy(players = state.players.map { PlayerPublicInfo.from(it) })
        broadcastAll(ServerMessage.PlayerLeft(playerId, playerName))
    }

    suspend fun addSpectator(player: Player, session: WebSocketSession) {
        val spectator = player.copy(isSpectator = true)
        spectatorConnections[spectator.id] = Pair(spectator, session)
        room = room.copy(spectators = spectatorConnections.values.map { PlayerPublicInfo.from(it.first) })
        broadcastAll(ServerMessage.SpectatorJoined(PlayerPublicInfo.from(spectator)))
        // Send current game state to spectator
        session.send(Frame.Text(ServerMessage.encode(ServerMessage.RoomUpdate(room))))
        if (state.phase != GamePhase.LOBBY) {
            session.send(Frame.Text(ServerMessage.encode(
                ServerMessage.PhaseChanged(state.phase, state.round, 0, state.alivePlayers.map { PlayerPublicInfo.from(it) })
            )))
        }
    }

    suspend fun initiateRematch(hostId: String) {
        if (hostId != room.hostId || state.phase != GamePhase.GAME_OVER) return
        rematchReady.clear()
        rematchReady.add(hostId)
        broadcastAll(ServerMessage.RematchInitiated(hostId))
        broadcastAll(ServerMessage.RematchReadyUpdate(rematchReady.toList(), connections.size))
    }

    suspend fun markRematchReady(playerId: String) {
        if (state.phase != GamePhase.GAME_OVER) return
        rematchReady.add(playerId)
        broadcastAll(ServerMessage.RematchReadyUpdate(rematchReady.toList(), connections.size))
        if (rematchReady.size >= connections.size) {
            startRematch()
        }
    }

    private suspend fun startRematch() {
        phaseTimerJob?.cancel()
        broadcastAll(ServerMessage.RematchStarting)
        rematchReady.clear()
        // Keep spectators but clear game state
        val humanPlayers = state.players.filter { !it.isAI }.map { it.copy(role = null, isAlive = true) }
        state = GameState(roomId = roomId, players = humanPlayers)
        room = room.copy(status = RoomStatus.WAITING, players = humanPlayers.map { PlayerPublicInfo.from(it) })
        gameStarted = false
        delay(1000)
        startGame()
    }

    fun getConnection(playerId: String): WebSocketSession? = connections[playerId]

    suspend fun startGame() {
        if (gameStarted) return
        gameStarted = true
        if (room.settings.allowAIFill && state.players.size < room.settings.maxPlayers) {
            val needed = room.settings.maxPlayers - state.players.size
            state = state.copy(players = state.players + AI_PERSONALITIES.shuffled().take(needed))
            room = room.copy(players = state.players.map { PlayerPublicInfo.from(it) })
        }
        state = state.copy(players = engine.assignRoles(state.players, room.settings))
        room = room.copy(status = RoomStatus.IN_GAME)
        broadcastAll(ServerMessage.GameStarted(state.players.size))
        state.players.forEach { p -> p.role?.let { sendTo(p.id, ServerMessage.RoleAssigned(it)) } }
        // Reveal mafia team to each mafia player
        val mafiaPlayers = state.players.filter { it.role?.isMafia() == true }
        mafiaPlayers.forEach { mafia ->
            val teammates = mafiaPlayers.filter { it.id != mafia.id }.map { PlayerPublicInfo.from(it) }
            sendTo(mafia.id, ServerMessage.MafiaTeamRevealed(teammates))
        }
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
            GamePhase.NIGHT -> {
                // Mafia MUST kill every night — force a random target if they didn't vote or tied
                if (state.nightActions.mafiaVotes.isEmpty() || state.nightActions.resolvedMafiaTarget() == null) {
                    val randomTarget = engine.pickRandomMafiaTarget(state)
                    if (randomTarget != null) {
                        state = state.copy(nightActions = state.nightActions.copy(mafiaVotes = mapOf("forced" to randomTarget)))
                    }
                }
                resolveNight()
            }
            GamePhase.NIGHT_RESULT -> transitionTo(GamePhase.DISCUSSION)
            GamePhase.DISCUSSION -> transitionTo(GamePhase.VOTING)
            GamePhase.VOTING -> resolveVoting()
            GamePhase.ELIMINATION -> { val w = state.checkWinCondition(); if (w != null) endGame(w) else transitionTo(GamePhase.NIGHT) }
            else -> {}
        }
    }

    suspend fun submitNightAction(playerId: String, targetId: String) {
        val player = state.getPlayer(playerId) ?: return
        // Non-mafia roles use the legacy night action path
        if (player.role?.isMafia() != true) {
            state = engine.submitNightAction(state, playerId, targetId)
            sendTo(playerId, ServerMessage.NightActionAck(true))
            if (player.role == Role.DETECTIVE) {
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
    }

    suspend fun submitMafiaVote(playerId: String, targetId: String) {
        val player = state.getPlayer(playerId) ?: return
        if (!player.isAlive || player.role?.isMafia() != true) return
        state = engine.submitMafiaVote(state, playerId, targetId)
        val tally = state.nightActions.mafiaVotes.values.groupingBy { it }.eachCount()
        broadcastToMafia(ServerMessage.MafiaVoteUpdate(playerId, targetId, tally))
        sendTo(playerId, ServerMessage.NightActionAck(true))

        if (engine.allMafiaVoted(state)) {
            val resolved = state.nightActions.resolvedMafiaTarget()
            if (resolved != null) {
                // Clear majority — check if all night actions are now done
                if (engine.allNightActionsSubmitted(state)) {
                    resolveMutex.withLock {
                        if (state.phase == GamePhase.NIGHT) { phaseTimerJob?.cancel(); resolveNight() }
                    }
                }
            } else {
                // Tie — reset mafia votes and ask them to re-vote
                val tieTally = state.nightActions.mafiaVotes.values.groupingBy { it }.eachCount()
                state = state.copy(nightActions = state.nightActions.copy(mafiaVotes = emptyMap()))
                broadcastToMafia(ServerMessage.MafiaVoteTie(tieTally))
            }
        }
    }

    suspend fun handleMafiaChat(senderId: String, text: String) {
        val sender = state.getPlayer(senderId) ?: return
        if (!sender.isAlive || sender.role?.isMafia() != true) return
        val message = ChatMessage(
            id = "${roomId}_mafia_${System.currentTimeMillis()}",
            senderId = senderId, senderName = sender.name,
            text = text, timestamp = System.currentTimeMillis(),
            round = state.round
        )
        state = state.copy(mafiaChatMessages = state.mafiaChatMessages + message)
        broadcastToMafia(ServerMessage.MafiaChatReceived(message))
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
        room = room.copy(settings = settings, maxPlayers = settings.maxPlayers)
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
        // Privately reveal all roles to the eliminated player so they can spectate with full info
        eliminated?.let { el ->
            val allRoles = state.players.mapNotNull { p -> p.role?.let { r -> p.id to r } }.toMap()
            sendTo(el.id, ServerMessage.EliminatedRolesReveal(allRoles))
        }
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
        // Privately reveal all roles to the eliminated player so they can spectate with full info
        eliminated?.let { el ->
            val allRoles = state.players.mapNotNull { p -> p.role?.let { r -> p.id to r } }.toMap()
            sendTo(el.id, ServerMessage.EliminatedRolesReveal(allRoles))
        }
        if (state.winner != null) endGame(state.winner!!) else transitionTo(GamePhase.ELIMINATION)
    }

    suspend fun handleChat(senderId: String, text: String) {
        val sender = state.getPlayer(senderId) ?: return
        if (!sender.isAlive && state.phase.isDayPhase()) return
        val message = ChatMessage(
            id = "${roomId}_${System.currentTimeMillis()}",
            senderId = senderId, senderName = sender.name,
            text = text, timestamp = System.currentTimeMillis(),
            round = state.round
        )
        state = state.copy(chatMessages = state.chatMessages + message)
        broadcastAll(ServerMessage.ChatReceived(message))

        // Trigger reactive AI replies when a human sends a message during Discussion
        if (state.phase == GamePhase.DISCUSSION && !sender.isAI && aiController != null) {
            val respondingBots = state.alivePlayers
                .filter { it.isAI }
                .shuffled()
                .take((1..2).random())
            respondingBots.forEach { bot ->
                scope.launch {
                    delay((4000L..10000L).random())
                    val reply = aiController.generateResponse(state, bot, message)
                    if (reply != null) handleChat(bot.id, reply)
                }
            }
        }
    }

    private suspend fun triggerAIActions(phase: GamePhase) {
        state.alivePlayers.filter { it.isAI }.forEach { player ->
            scope.launch {
                delay((1000L..3000L).random())
                aiController?.let { ai ->
                    when (phase) {
                        GamePhase.NIGHT -> {
                            val target = ai.chooseNightTarget(state, player)
                            if (target != null) {
                                if (player.role?.isMafia() == true) submitMafiaVote(player.id, target)
                                else submitNightAction(player.id, target)
                            }
                        }
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
        spectatorConnections.forEach { (_, pair) -> scope.launch { try { pair.second.send(Frame.Text(encoded)) } catch (_: Exception) {} } }
    }

    private fun broadcastToMafia(message: ServerMessage) {
        val encoded = ServerMessage.encode(message)
        state.aliveMafia.forEach { mafia ->
            connections[mafia.id]?.let { s -> scope.launch { try { s.send(Frame.Text(encoded)) } catch (_: Exception) {} } }
        }
    }

    private fun sendTo(playerId: String, message: ServerMessage) {
        connections[playerId]?.let { s -> scope.launch { try { s.send(Frame.Text(ServerMessage.encode(message))) } catch (_: Exception) {} } }
    }
}
