package com.mafia.shared.game

import com.mafia.shared.model.*

class GameEngine {

    fun assignRoles(players: List<Player>, settings: GameSettings = GameSettings()): List<Player> {
        val roles = RoleDistribution.forPlayerCount(players.size, settings).shuffled()
        return players.zip(roles) { player, role -> player.assignRole(role) }
    }

    fun processNightActions(state: GameState): GameState {
        val resolution = state.nightActions.resolve()
        var s = state.copy(
            savedThisNight = resolution.wasSaved,
            eliminatedThisRound = resolution.eliminatedId,
            vigilanteEliminated = null
        )
        resolution.eliminatedId?.let { s = s.updatePlayer(it) { p -> p.eliminate() } }

        // Vigilante kill resolution
        val vigilanteTarget = resolution.vigilanteTargetId
        if (vigilanteTarget != null) {
            val target = s.getPlayer(vigilanteTarget)
            if (target != null && target.isAlive) {
                s = s.updatePlayer(vigilanteTarget) { p -> p.eliminate() }
                // If target is Town (innocent), vigilante dies too
                if (target.role?.isTown() == true) {
                    val vigilante = s.alivePlayers.find { it.role == Role.VIGILANTE }
                    if (vigilante != null) {
                        s = s.copy(vigilanteEliminated = vigilante.id)
                        s = s.updatePlayer(vigilante.id) { p -> p.eliminate() }
                    }
                }
            }
        }

        val winner = s.checkWinCondition()
        if (winner != null) s = s.copy(winner = winner, phase = GamePhase.GAME_OVER)
        return s
    }

    fun processVote(state: GameState): GameState {
        val eliminatedId = if (state.ministerVetoThisRound) null else state.voteResult()
        var s = state.copy(eliminatedThisRound = eliminatedId, ministerVetoThisRound = false)
        eliminatedId?.let { s = s.updatePlayer(it) { p -> p.eliminate() } }
        val winner = s.checkWinCondition()
        if (winner != null) s = s.copy(winner = winner, phase = GamePhase.GAME_OVER)
        return s
    }

    fun advancePhase(state: GameState): GameState {
        val next = state.phase.next()
        return when (next) {
            GamePhase.NIGHT -> state.copy(phase = next, round = state.round + 1, nightActions = NightActions(), votes = emptyMap(), skips = emptySet(), eliminatedThisRound = null, vigilanteEliminated = null, savedThisNight = false, ministerVetoThisRound = false)
            GamePhase.DISCUSSION -> state.copy(phase = next, votes = emptyMap(), skips = emptySet(), eliminatedThisRound = null)
            GamePhase.VOTING -> state.copy(phase = next, votes = emptyMap(), skips = emptySet())
            else -> state.copy(phase = next)
        }
    }

    fun submitNightAction(state: GameState, playerId: String, targetId: String): GameState {
        val player = state.getPlayer(playerId) ?: return state
        if (!player.isAlive) return state
        val target = state.getPlayer(targetId)
        if (target == null || !target.isAlive) return state
        val actions = when (player.role) {
            Role.MAFIA -> state.nightActions.copy(mafiaTarget = targetId)
            Role.DETECTIVE -> state.nightActions.copy(detectiveInvestigate = targetId)
            Role.DOCTOR -> state.nightActions.copy(doctorProtect = targetId)
            Role.VIGILANTE -> state.nightActions.copy(vigilanteTarget = targetId)
            Role.ESCORT -> state.nightActions.copy(escortBlock = targetId)
            else -> return state
        }
        return state.copy(nightActions = actions)
    }

    fun submitVote(state: GameState, voterId: String, targetId: String): GameState {
        val voter = state.getPlayer(voterId)
        val target = state.getPlayer(targetId)
        if (voter == null || !voter.isAlive || target == null || !target.isAlive) return state
        return state.copy(votes = state.votes + (voterId to targetId))
    }

    fun submitSkip(state: GameState, voterId: String): GameState {
        val voter = state.getPlayer(voterId)
        if (voter == null || !voter.isAlive) return state
        return state.copy(skips = state.skips + voterId)
    }

    fun submitMinisterVeto(state: GameState, playerId: String): GameState {
        val player = state.getPlayer(playerId) ?: return state
        if (!player.isAlive || player.role != Role.MINISTER) return state
        if (state.ministerVetoUsed) return state
        return state.copy(ministerVetoUsed = true, ministerVetoThisRound = true)
    }

    fun allNightActionsSubmitted(state: GameState): Boolean {
        return state.alivePlayers.filter { it.role?.hasNightAction == true }.all { player ->
            when (player.role) {
                Role.MAFIA -> state.nightActions.mafiaTarget != null
                Role.DETECTIVE -> state.nightActions.detectiveInvestigate != null
                Role.DOCTOR -> state.nightActions.doctorProtect != null
                Role.VIGILANTE -> state.nightActions.vigilanteTarget != null
                Role.ESCORT -> state.nightActions.escortBlock != null
                else -> true
            }
        }
    }

    fun allVotedOrSkipped(state: GameState): Boolean {
        val aliveIds = state.alivePlayers.map { it.id }.toSet()
        return aliveIds.all { it in state.votes || it in state.skips }
    }

    fun getValidNightTargets(state: GameState, playerId: String): List<Player> {
        val player = state.getPlayer(playerId) ?: return emptyList()
        return when (player.role) {
            Role.MAFIA -> state.alivePlayers.filter { it.role?.isMafia() != true }
            Role.DETECTIVE -> state.alivePlayers.filter { it.id != playerId }
            Role.DOCTOR -> state.alivePlayers
            Role.VIGILANTE -> state.alivePlayers.filter { it.id != playerId }
            Role.ESCORT -> state.alivePlayers.filter { it.id != playerId }
            else -> emptyList()
        }
    }
}
