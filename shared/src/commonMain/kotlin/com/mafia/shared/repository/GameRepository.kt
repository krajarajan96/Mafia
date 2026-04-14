package com.mafia.shared.repository

import com.mafia.shared.game.GameEngine
import com.mafia.shared.model.*
import com.mafia.shared.network.GameSocket
import com.mafia.shared.network.messages.ClientMessage
import com.mafia.shared.network.messages.ServerMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private const val SKIP_VOTE = "SKIP"

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
    private val _mafiaChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val mafiaChatMessages: StateFlow<List<ChatMessage>> = _mafiaChatMessages.asStateFlow()
    private val _mafiaTeammates = MutableStateFlow<List<PlayerPublicInfo>>(emptyList())
    val mafiaTeammates: StateFlow<List<PlayerPublicInfo>> = _mafiaTeammates.asStateFlow()
    private val _mafiaVoteTally = MutableStateFlow<Map<String, Int>>(emptyMap())
    val mafiaVoteTally: StateFlow<Map<String, Int>> = _mafiaVoteTally.asStateFlow()
    private val _mafiaVoteTie = MutableStateFlow(false)
    val mafiaVoteTie: StateFlow<Boolean> = _mafiaVoteTie.asStateFlow()
    private val _timer = MutableStateFlow(0)
    val timer: StateFlow<Int> = _timer.asStateFlow()
    private val _lastEvent = MutableSharedFlow<ServerMessage>(replay = 1)
    val lastEvent: SharedFlow<ServerMessage> = _lastEvent.asSharedFlow()
    private val _detectiveResult = MutableStateFlow<ServerMessage.DetectiveResult?>(null)
    val detectiveResult: StateFlow<ServerMessage.DetectiveResult?> = _detectiveResult.asStateFlow()
    private val _voteTally = MutableStateFlow<Map<String, Int>>(emptyMap())
    val voteTally: StateFlow<Map<String, Int>> = _voteTally.asStateFlow()
    private val _nightSummary = MutableStateFlow<ServerMessage.NightSummary?>(null)
    val nightSummary: StateFlow<ServerMessage.NightSummary?> = _nightSummary.asStateFlow()
    private val _voteResult = MutableStateFlow<ServerMessage.VoteResult?>(null)
    val voteResult: StateFlow<ServerMessage.VoteResult?> = _voteResult.asStateFlow()
    private val _voteLog = MutableStateFlow<List<VoteEntry>>(emptyList())
    val voteLog: StateFlow<List<VoteEntry>> = _voteLog.asStateFlow()
    private val _eventLog = MutableStateFlow<List<GameEvent>>(emptyList())
    val eventLog: StateFlow<List<GameEvent>> = _eventLog.asStateFlow()
    private val _ministerVetoUsed = MutableStateFlow(false)
    val ministerVetoUsed: StateFlow<Boolean> = _ministerVetoUsed.asStateFlow()
    private val _revealedRoles = MutableStateFlow<Map<String, Role>>(emptyMap())
    val revealedRoles: StateFlow<Map<String, Role>> = _revealedRoles.asStateFlow()
    /** All players who started the game — never cleared mid-game; used for name lookups after elimination. */
    private val _allPlayers = MutableStateFlow<List<PlayerPublicInfo>>(emptyList())
    val allPlayers: StateFlow<List<PlayerPublicInfo>> = _allPlayers.asStateFlow()
    private val _spectators = MutableStateFlow<List<PlayerPublicInfo>>(emptyList())
    val spectators: StateFlow<List<PlayerPublicInfo>> = _spectators.asStateFlow()
    private val _rematchInitiated = MutableStateFlow(false)
    val rematchInitiated: StateFlow<Boolean> = _rematchInitiated.asStateFlow()
    private val _rematchReadyIds = MutableStateFlow<List<String>>(emptyList())
    val rematchReadyIds: StateFlow<List<String>> = _rematchReadyIds.asStateFlow()
    private val _rematchTotalPlayers = MutableStateFlow(0)
    val rematchTotalPlayers: StateFlow<Int> = _rematchTotalPlayers.asStateFlow()
    val connectionState = socket.connectionState

    // ── Local single-player state ───────────────────────────────────────────
    private var isLocalGame = false
    private var localState: GameState? = null
    private var localSettings: GameSettings = GameSettings()
    private var localGameJob: Job? = null
    private var pendingNightAction: CompletableDeferred<String>? = null
    private var pendingMafiaVote: CompletableDeferred<String>? = null
    private var pendingVote: CompletableDeferred<String>? = null
    private var localMsgCounter = 0
    private var localPlayerName: String = ""
    private var localPlayerEmoji: String = ""

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
                if (msg.phase == GamePhase.ROLE_REVEAL) _allPlayers.value = msg.alivePlayers
                if (msg.phase == GamePhase.NIGHT) {
                    _detectiveResult.value = null; _nightSummary.value = null
                    _mafiaVoteTally.value = emptyMap(); _mafiaVoteTie.value = false
                }
                if (msg.phase == GamePhase.VOTING) _voteLog.value = emptyList()
            }
            is ServerMessage.TimerTick -> _timer.value = msg.secondsRemaining
            is ServerMessage.ChatReceived -> _chatMessages.value = _chatMessages.value + msg.message
            is ServerMessage.MafiaChatReceived -> _mafiaChatMessages.value = _mafiaChatMessages.value + msg.message
            is ServerMessage.MafiaTeamRevealed -> _mafiaTeammates.value = msg.teammates
            is ServerMessage.MafiaVoteUpdate -> {
                _mafiaVoteTally.value = msg.tally
                _mafiaVoteTie.value = false
            }
            is ServerMessage.MafiaVoteTie -> {
                _mafiaVoteTally.value = msg.tally
                _mafiaVoteTie.value = true
            }
            is ServerMessage.DetectiveResult -> _detectiveResult.value = msg
            is ServerMessage.NightSummary -> {
                if (msg.eliminatedPlayer != null && msg.eliminatedRole != null)
                    _revealedRoles.value = _revealedRoles.value + (msg.eliminatedPlayer.id to msg.eliminatedRole)
                _nightSummary.value = msg
                val round = _round.value
                val desc = when {
                    msg.wasSaved -> "Someone was saved by the Doctor"
                    msg.eliminatedPlayer != null -> "${msg.eliminatedPlayer.name} was killed by the Mafia"
                    else -> "A quiet night — no one was harmed"
                }
                val vigilanteDesc = if (msg.vigilanteKilled != null) {
                    " | Vigilante shot ${msg.vigilanteKilled.name}" +
                        if (msg.vigilanteEliminated != null) " (backfired — Vigilante died too)" else ""
                } else ""
                _eventLog.value = _eventLog.value + GameEvent(round, "Night $round", desc + vigilanteDesc, msg.eliminatedPlayer != null)
            }
            is ServerMessage.VoteUpdate -> {
                _voteTally.value = msg.currentTally
                val voterName = _alivePlayers.value.find { it.id == msg.voterId }?.name ?: msg.voterId
                val isSkip = msg.targetId == SKIP_VOTE
                val targetName = if (isSkip) "abstained" else (_alivePlayers.value.find { it.id == msg.targetId }?.name ?: msg.targetId)
                _voteLog.value = _voteLog.value + VoteEntry(voterName, targetName, isSkip)
                val round = _round.value
                val voteDesc = if (isSkip) "$voterName abstained" else "$voterName → $targetName"
                _eventLog.value = _eventLog.value + GameEvent(round, "Vote", voteDesc, false)
            }
            is ServerMessage.VoteResult -> {
                if (msg.eliminatedPlayer != null && msg.eliminatedRole != null)
                    _revealedRoles.value = _revealedRoles.value + (msg.eliminatedPlayer.id to msg.eliminatedRole)
                _voteResult.value = msg
                val round = _round.value
                val desc = when {
                    msg.eliminatedPlayer != null -> "${msg.eliminatedPlayer.name} was eliminated by vote"
                    msg.wasTie -> "Vote ended in a tie — no elimination"
                    else -> "No elimination (vote failed or vetoed)"
                }
                _eventLog.value = _eventLog.value + GameEvent(round, "Vote $round", desc, msg.eliminatedPlayer != null)
            }
            is ServerMessage.GameOver -> _phase.value = GamePhase.GAME_OVER
            is ServerMessage.SettingsUpdated -> _room.value = _room.value?.copy(settings = msg.settings)
            is ServerMessage.SpectatorJoined -> _spectators.value = _spectators.value + msg.spectator
            is ServerMessage.SpectatorLeft -> _spectators.value = _spectators.value.filter { it.id != msg.spectatorId }
            is ServerMessage.RematchInitiated -> {
                _rematchInitiated.value = true
                _rematchReadyIds.value = listOf(msg.hostId)
            }
            is ServerMessage.RematchReadyUpdate -> {
                _rematchReadyIds.value = msg.readyPlayerIds
                _rematchTotalPlayers.value = msg.totalPlayers
            }
            is ServerMessage.RematchStarting -> {
                _rematchInitiated.value = false
                _rematchReadyIds.value = emptyList()
                resetForNewGame()
            }
            else -> {}
        }
    }

    // ── Online actions ──────────────────────────────────────────────────────
    fun createRoom(mode: GameMode, name: String, emoji: String = "🕵️") =
        socket.send(ClientMessage.CreateRoom(mode, name, emoji))

    fun joinRoom(code: String, name: String, emoji: String = "🕵️") =
        socket.send(ClientMessage.JoinRoom(code, name, emoji))

    fun startGame() {
        if (isLocalGame) {
            localGameJob?.cancel()
            localGameJob = scope.launch { runLocalGame(localPlayerName, localPlayerEmoji) }
        } else {
            socket.send(ClientMessage.StartGame)
        }
    }

    fun accuse(targetId: String, reason: String) = socket.send(ClientMessage.Accuse(targetId, reason))

    fun submitNightAction(targetId: String) {
        if (isLocalGame) {
            // Mafia players use the mafia vote path
            val humanRole = _myRole.value
            if (humanRole?.isMafia() == true) pendingMafiaVote?.complete(targetId)
            else pendingNightAction?.complete(targetId)
        } else {
            // Non-mafia roles use NightAction; mafia uses MafiaVote
            if (_myRole.value?.isMafia() == true) socket.send(ClientMessage.MafiaVote(targetId))
            else socket.send(ClientMessage.NightAction(targetId))
        }
    }

    fun sendChat(text: String) {
        if (isLocalGame) {
            val playerId = _myPlayerId.value ?: return
            val name = localState?.getPlayer(playerId)?.name ?: "You"
            val msg = ChatMessage(
                id = "local_${localMsgCounter++}",
                senderId = playerId, senderName = name,
                text = text, timestamp = localMsgCounter.toLong(),
                round = _round.value
            )
            _chatMessages.value = _chatMessages.value + msg
        } else {
            socket.send(ClientMessage.SendChat(text))
        }
    }

    fun sendMafiaChat(text: String) {
        if (isLocalGame) {
            val playerId = _myPlayerId.value ?: return
            val name = localState?.getPlayer(playerId)?.name ?: "You"
            val msg = ChatMessage(
                id = "local_mafia_${localMsgCounter++}",
                senderId = playerId, senderName = name,
                text = text, timestamp = localMsgCounter.toLong(),
                round = _round.value
            )
            _mafiaChatMessages.value = _mafiaChatMessages.value + msg
        } else {
            socket.send(ClientMessage.MafiaChat(text))
        }
    }

    fun castVote(targetId: String) {
        if (isLocalGame) pendingVote?.complete(targetId)
        else socket.send(ClientMessage.CastVote(targetId))
    }

    fun skipVote() {
        if (isLocalGame) pendingVote?.complete(SKIP_VOTE)
        else socket.send(ClientMessage.SkipVote)
    }

    fun useMinisterVeto() {
        if (isLocalGame) {
            val playerId = _myPlayerId.value ?: return
            localState = engine.submitMinisterVeto(localState ?: return, playerId)
            _ministerVetoUsed.value = localState?.ministerVetoUsed == true
            pendingVote?.complete(SKIP_VOTE)
        } else {
            socket.send(ClientMessage.UseVeto)
        }
    }

    fun joinAsSpectator(code: String, name: String, emoji: String = "👁️") =
        socket.send(ClientMessage.JoinAsSpectator(code, name, emoji))

    fun initiateRematch() = socket.send(ClientMessage.RematchVote(true))

    fun markRematchReady() = socket.send(ClientMessage.RematchVote(true))

    fun updateSettings(settings: GameSettings) {
        if (isLocalGame) localSettings = settings
        else socket.send(ClientMessage.UpdateSettings(settings))
    }

    fun leaveRoom() {
        if (isLocalGame) {
            localGameJob?.cancel()
            isLocalGame = false; localState = null
        } else {
            socket.send(ClientMessage.LeaveRoom)
        }
        _room.value = null; _myRole.value = null; _phase.value = GamePhase.LOBBY
        _chatMessages.value = emptyList(); _alivePlayers.value = emptyList()
        _mafiaChatMessages.value = emptyList(); _mafiaTeammates.value = emptyList()
    }

    fun resetForNewGame() {
        isLocalGame = false; localState = null; localGameJob?.cancel()
        _myRole.value = null; _phase.value = GamePhase.LOBBY
        _chatMessages.value = emptyList(); _mafiaChatMessages.value = emptyList()
        _mafiaTeammates.value = emptyList(); _mafiaVoteTally.value = emptyMap(); _mafiaVoteTie.value = false
        _detectiveResult.value = null; _voteTally.value = emptyMap()
        _nightSummary.value = null; _voteResult.value = null; _voteLog.value = emptyList()
        _round.value = 0; _alivePlayers.value = emptyList(); _allPlayers.value = emptyList()
        _eventLog.value = emptyList(); _ministerVetoUsed.value = false; _revealedRoles.value = emptyMap()
        _spectators.value = emptyList(); _rematchInitiated.value = false
        _rematchReadyIds.value = emptyList(); _rematchTotalPlayers.value = 0
    }

    // ── Local single-player game loop ───────────────────────────────────────

    fun prepareLocalGame(playerName: String, playerEmoji: String) {
        isLocalGame = true
        localPlayerName = playerName; localPlayerEmoji = playerEmoji
        localSettings = GameSettings()
        val humanId = "local_player"
        _myPlayerId.value = humanId
        val humanInfo = PlayerPublicInfo(id = humanId, name = playerName, avatarEmoji = playerEmoji, isHost = true, isAI = false, isAlive = true, isConnected = true)
        val fakeRoom = Room(id = "local", code = "LOCAL", hostId = humanId, mode = GameMode.SINGLE_PLAYER, players = listOf(humanInfo))
        _room.value = fakeRoom
        scope.launch { _lastEvent.emit(ServerMessage.RoomCreated(fakeRoom, humanId)) }
    }

    fun startLocalGame(playerName: String, playerEmoji: String, settings: GameSettings = GameSettings()) {
        isLocalGame = true; localPlayerName = playerName; localPlayerEmoji = playerEmoji
        localSettings = settings; localGameJob?.cancel()
        localGameJob = scope.launch { runLocalGame(playerName, playerEmoji) }
    }

    private suspend fun runLocalGame(playerName: String, playerEmoji: String) {
        val humanId = "local_player"
        val human = Player(id = humanId, name = playerName, avatarEmoji = playerEmoji, isHost = true)
        val bots = AI_PERSONALITIES.shuffled().take(localSettings.botCount)
        val allPlayers = engine.assignRoles(listOf(human) + bots, localSettings)

        localState = GameState(roomId = "local", players = allPlayers, round = 0)
        _myPlayerId.value = humanId
        val humanRole = allPlayers.first { it.id == humanId }.role
        _myRole.value = humanRole
        val publicPlayers = allPlayers.map { PlayerPublicInfo.from(it) }
        _alivePlayers.value = publicPlayers; _allPlayers.value = publicPlayers
        _chatMessages.value = emptyList(); _mafiaChatMessages.value = emptyList()
        _round.value = 0; _voteTally.value = emptyMap(); _voteLog.value = emptyList()
        _detectiveResult.value = null; _nightSummary.value = null; _voteResult.value = null
        _eventLog.value = emptyList(); _ministerVetoUsed.value = false
        _mafiaVoteTally.value = emptyMap(); _mafiaVoteTie.value = false

        // Reveal mafia teammates to human if they are mafia
        if (humanRole?.isMafia() == true) {
            _mafiaTeammates.value = allPlayers
                .filter { it.role?.isMafia() == true && it.id != humanId }
                .map { PlayerPublicInfo.from(it) }
        }

        _lastEvent.emit(ServerMessage.GameStarted(allPlayers.size))
        localPhase(GamePhase.ROLE_REVEAL, GamePhase.ROLE_REVEAL.defaultDurationSeconds)

        var round = 0
        while (true) {
            localState!!.checkWinCondition()?.let { endLocalGame(it); return }

            round++
            localState = localState!!.copy(
                round = round, nightActions = NightActions(),
                votes = emptyMap(), skips = emptySet(),
                eliminatedThisRound = null, vigilanteEliminated = null,
                savedThisNight = false, ministerVetoThisRound = false
            )
            _detectiveResult.value = null; _voteTally.value = emptyMap()
            _mafiaVoteTally.value = emptyMap(); _mafiaVoteTie.value = false

            // ── NIGHT ───────────────────────────────────────────────────────
            val humanPlayer = localState!!.getPlayer(humanId)
            val humanIsMafia = humanPlayer?.role?.isMafia() == true

            pendingNightAction = if (!humanIsMafia && humanPlayer?.role?.hasNightAction == true) CompletableDeferred() else null
            pendingMafiaVote = if (humanIsMafia) CompletableDeferred() else null
            val nightDeferred = pendingNightAction
            val mafiaVoteDeferred = pendingMafiaVote

            localPhaseWithWork(GamePhase.NIGHT, GamePhase.NIGHT.defaultDurationSeconds) {
                doAINightActions()
                if (humanPlayer?.isAlive == true) {
                    when {
                        humanIsMafia -> mafiaVoteDeferred?.await()
                        humanPlayer.role?.hasNightAction == true -> nightDeferred?.await()
                    }
                }
            }
            pendingNightAction = null; pendingMafiaVote = null

            // Apply human mafia vote
            if (humanIsMafia && mafiaVoteDeferred?.isCompleted == true) {
                try {
                    val target = mafiaVoteDeferred.getCompleted()
                    localState = engine.submitMafiaVote(localState!!, humanId, target)
                    _mafiaVoteTally.value = localState!!.nightActions.mafiaVotes.values.groupingBy { it }.eachCount()
                } catch (_: Exception) {}
            }
            // Apply human non-mafia night action
            if (!humanIsMafia && nightDeferred?.isCompleted == true) {
                try {
                    val target = nightDeferred.getCompleted()
                    localState = engine.submitNightAction(localState!!, humanId, target)
                    if (localState!!.getPlayer(humanId)?.role == Role.DETECTIVE) {
                        localState!!.getPlayer(target)?.let { t ->
                            _detectiveResult.value = ServerMessage.DetectiveResult(t.id, t.name, t.role?.isMafia() == true)
                        }
                    }
                } catch (_: Exception) {}
            }
            // Tie-break for mafia if needed (local game: just pick random)
            if (localState!!.nightActions.mafiaVotes.isNotEmpty() && localState!!.nightActions.resolvedMafiaTarget() == null) {
                val randomTarget = engine.pickRandomMafiaTarget(localState!!)
                if (randomTarget != null) {
                    localState = localState!!.copy(nightActions = localState!!.nightActions.copy(mafiaVotes = mapOf("resolved" to randomTarget)))
                }
            }

            localState = engine.processNightActions(localState!!)
            if (localState!!.getPlayer(humanId)?.isAlive == false && _revealedRoles.value.size < localState!!.players.size) {
                _revealedRoles.value = localState!!.players.associate { it.id to (it.role ?: Role.TOWNSFOLK) }
            }
            val nightElim = localState!!.eliminatedThisRound?.let { localState!!.getPlayer(it) }
            val vigilanteKilledPlayer = localState!!.nightActions.resolve().vigilanteTargetId?.let { localState!!.getPlayer(it) }
            val vigilanteEliminatedPlayer = localState!!.vigilanteEliminated?.let { localState!!.getPlayer(it) }
            val summary = ServerMessage.NightSummary(
                nightElim?.let { PlayerPublicInfo.from(it) }, nightElim?.role, localState!!.savedThisNight,
                vigilanteKilledPlayer?.let { PlayerPublicInfo.from(it) },
                vigilanteEliminatedPlayer?.let { PlayerPublicInfo.from(it) }
            )
            _nightSummary.value = summary; _lastEvent.emit(summary)
            val nightDesc = when {
                summary.wasSaved -> "Someone was saved by the Doctor"
                summary.eliminatedPlayer != null -> "${summary.eliminatedPlayer.name} was killed by the Mafia"
                else -> "A quiet night — no one was harmed"
            }
            _eventLog.value = _eventLog.value + GameEvent(round, "Night $round", nightDesc, summary.eliminatedPlayer != null)

            localState!!.checkWinCondition()?.let { endLocalGame(it); return }

            // ── NIGHT RESULT ─────────────────────────────────────────────────
            localPhase(GamePhase.NIGHT_RESULT, GamePhase.NIGHT_RESULT.defaultDurationSeconds)

            // ── DISCUSSION ───────────────────────────────────────────────────
            // NOTE: Chat is NOT cleared here — persistent across rounds
            localPhase(GamePhase.DISCUSSION, GamePhase.DISCUSSION.defaultDurationSeconds)

            // ── VOTING ───────────────────────────────────────────────────────
            localState = localState!!.copy(votes = emptyMap(), skips = emptySet(), ministerVetoThisRound = false)
            _voteLog.value = emptyList()
            pendingVote = CompletableDeferred()
            val voteDeferred = pendingVote!!
            localPhaseWithWork(GamePhase.VOTING, GamePhase.VOTING.defaultDurationSeconds) {
                doAIVotes()
                val hp = localState!!.getPlayer(humanId)
                if (hp?.isAlive == true) voteDeferred.await()
            }
            pendingVote = null

            if (voteDeferred.isCompleted) {
                try {
                    val target = voteDeferred.getCompleted()
                    val humanName = localState!!.getPlayer(humanId)?.name ?: "You"
                    val voteRound = _round.value
                    if (target == SKIP_VOTE) {
                        localState = engine.submitSkip(localState!!, humanId)
                        _voteLog.value = _voteLog.value + VoteEntry(humanName, "Skip", true)
                        _eventLog.value = _eventLog.value + GameEvent(voteRound, "Vote", "$humanName abstained", false)
                    } else {
                        localState = engine.submitVote(localState!!, humanId, target)
                        _voteTally.value = localState!!.votes.values.groupingBy { it }.eachCount()
                        val targetName = localState!!.getPlayer(target)?.name ?: target
                        _voteLog.value = _voteLog.value + VoteEntry(humanName, targetName, false)
                        _eventLog.value = _eventLog.value + GameEvent(voteRound, "Vote", "$humanName → $targetName", false)
                    }
                } catch (_: Exception) {}
            }

            val eliminatedVId = localState!!.voteResult()
            localState = engine.processVote(localState!!)
            if (localState!!.getPlayer(humanId)?.isAlive == false && _revealedRoles.value.size < localState!!.players.size) {
                _revealedRoles.value = localState!!.players.associate { it.id to (it.role ?: Role.TOWNSFOLK) }
            }
            val eliminatedV = eliminatedVId?.let { localState!!.getPlayer(it) }
            val tally = localState!!.votes.values.groupingBy { it }.eachCount()
            val voteResultMsg = ServerMessage.VoteResult(
                eliminatedV?.let { PlayerPublicInfo.from(it) }, eliminatedV?.role, eliminatedVId == null, tally
            )
            _voteResult.value = voteResultMsg; _lastEvent.emit(voteResultMsg)
            val voteDesc = if (eliminatedV != null) "${eliminatedV.name} was eliminated by vote" else "No elimination (tie or veto)"
            _eventLog.value = _eventLog.value + GameEvent(round, "Vote $round", voteDesc, eliminatedV != null)

            localState!!.checkWinCondition()?.let { endLocalGame(it); return }

            // ── ELIMINATION ──────────────────────────────────────────────────
            localPhase(GamePhase.ELIMINATION, GamePhase.ELIMINATION.defaultDurationSeconds)
            _alivePlayers.value = localState!!.alivePlayers.map { PlayerPublicInfo.from(it) }

            localState!!.checkWinCondition()?.let { endLocalGame(it); return }
        }
    }

    private suspend fun localPhase(phase: GamePhase, durationSec: Int) {
        val state = localState!!
        _phase.value = phase; _round.value = state.round
        _alivePlayers.value = state.alivePlayers.map { PlayerPublicInfo.from(it) }
        _timer.value = durationSec
        repeat(durationSec) { i -> delay(1000); _timer.value = durationSec - i - 1 }
    }

    private suspend fun localPhaseWithWork(phase: GamePhase, durationSec: Int, block: suspend () -> Unit) {
        val state = localState!!
        _phase.value = phase; _round.value = state.round
        _alivePlayers.value = state.alivePlayers.map { PlayerPublicInfo.from(it) }
        _timer.value = durationSec
        val timerJob = scope.launch {
            repeat(durationSec) { i -> delay(1000); _timer.value = durationSec - i - 1 }
        }
        try { withTimeoutOrNull(durationSec * 1000L) { block() } }
        finally { timerJob.cancel(); _timer.value = 0 }
    }

    private fun doAINightActions() {
        val bots = localState?.alivePlayers?.filter { it.isAI && it.role?.hasNightAction == true } ?: return
        bots.forEach { bot ->
            val state = localState ?: return
            val target = when (bot.role) {
                Role.MAFIA -> state.alivePlayers.filter { it.role?.isMafia() != true }.randomOrNull()?.id
                Role.DETECTIVE -> state.alivePlayers.filter { it.id != bot.id }.randomOrNull()?.id
                Role.DOCTOR -> state.alivePlayers.randomOrNull()?.id
                Role.VIGILANTE -> state.alivePlayers.filter { it.id != bot.id }.randomOrNull()?.id
                Role.ESCORT -> state.alivePlayers.filter { it.id != bot.id }.randomOrNull()?.id
                else -> null
            }
            if (target != null) {
                localState = if (bot.role?.isMafia() == true)
                    engine.submitMafiaVote(localState!!, bot.id, target)
                else
                    engine.submitNightAction(localState!!, bot.id, target)
            }
        }
        // Update mafia vote tally after bots vote
        val tally = localState?.nightActions?.mafiaVotes?.values?.groupingBy { it }?.eachCount() ?: emptyMap()
        if (tally.isNotEmpty()) _mafiaVoteTally.value = tally
    }

    private fun doAIVotes() {
        val bots = localState?.alivePlayers?.filter { it.isAI } ?: return
        bots.forEach { bot ->
            val state = localState ?: return
            val target = state.alivePlayers.filter { it.id != bot.id }.randomOrNull()?.id
            target?.let {
                localState = engine.submitVote(localState!!, bot.id, it)
                _voteTally.value = localState!!.votes.values.groupingBy { v -> v }.eachCount()
                val targetName = localState!!.getPlayer(it)?.name ?: it
                _voteLog.value = _voteLog.value + VoteEntry(bot.name, targetName, false)
                val round = _round.value
                _eventLog.value = _eventLog.value + GameEvent(round, "Vote", "${bot.name} → $targetName", false)
            }
        }
    }

    private suspend fun endLocalGame(winner: Team) {
        val state = localState ?: return
        _phase.value = GamePhase.GAME_OVER
        _lastEvent.emit(ServerMessage.GameOver(winner, state.players.associate { it.id to (it.role ?: Role.TOWNSFOLK) }))
    }
}
