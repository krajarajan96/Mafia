package com.mafia.shared.game

import com.mafia.shared.model.*

class GameEngine {

    fun assignRoles(players: List<Player>): List<Player> {
        val distribution = RoleDistribution.forPlayerCount(players.size)
        val roles = distribution.toRoleList().shuffled()
        return players.zip(roles) { player, role -> player.assignRole(role) }
    }

    fun processNightActions(state: GameState): GameState {
        val resolution = state.nightActions.resolve()
        var s = state.copy(savedThisNight = resolution.wasSaved, eliminatedThisRound = resolution.eliminatedId)
        resolution.eliminatedId?.let { s = s.updatePlayer(it) { p -> p.eliminate() } }
        val winner = s.checkWinCondition()
        if (winner != null) s = s.copy(winner = winner, phase = GamePhase.GAME_OVER)
        return s
    }

    fun processVote(state: GameState): GameState {
        val eliminatedId = state.voteResult()
        var s = state.copy(eliminatedThisRound = eliminatedId)
        eliminatedId?.let { s = s.updatePlayer(it) { p -> p.eliminate() } }
        val winner = s.checkWinCondition()
        if (winner != null) s = s.copy(winner = winner, phase = GamePhase.GAME_OVER)
        return s
    }

    fun advancePhase(state: GameState): GameState {
        val next = state.phase.next()
        return when (next) {
            GamePhase.NIGHT -> state.copy(phase = next, round = state.round + 1, nightActions = NightActions(), votes = emptyMap(), eliminatedThisRound = null, savedThisNight = false)
            GamePhase.DISCUSSION -> state.copy(phase = next, votes = emptyMap(), eliminatedThisRound = null)
            GamePhase.VOTING -> state.copy(phase = next, votes = emptyMap())
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

    fun allNightActionsSubmitted(state: GameState): Boolean {
        return state.alivePlayers.filter { it.role?.hasNightAction == true }.all { player ->
            when (player.role) {
                Role.MAFIA -> state.nightActions.mafiaTarget != null
                Role.DETECTIVE -> state.nightActions.detectiveInvestigate != null
                Role.DOCTOR -> state.nightActions.doctorProtect != null
                else -> true
            }
        }
    }

    fun allVotesSubmitted(state: GameState): Boolean {
        val aliveIds = state.alivePlayers.map { it.id }.toSet()
        return aliveIds.all { it in state.votes }
    }

    fun getValidNightTargets(state: GameState, playerId: String): List<Player> {
        val player = state.getPlayer(playerId) ?: return emptyList()
        return when (player.role) {
            Role.MAFIA -> state.alivePlayers.filter { it.role?.isMafia() != true }
            Role.DETECTIVE -> state.alivePlayers.filter { it.id != playerId }
            Role.DOCTOR -> state.alivePlayers
            else -> emptyList()
        }
    }
}
