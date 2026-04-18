package com.mafia.server.game

import com.mafia.server.ai.GameAI
import com.mafia.shared.model.*

/**
 * Coordinates AI player actions within a game session.
 * All decision logic is delegated to the injected [GameAI] implementation
 * (e.g. GroqGameAI), with a simple heuristic fallback when no AI is configured.
 */
class AIPlayerController(private val ai: GameAI? = null) {

    suspend fun chooseNightTarget(state: GameState, player: Player): String? =
        ai?.chooseNightTarget(state, player) ?: heuristicNightTarget(state, player)

    suspend fun generateDiscussion(state: GameState, player: Player): List<String> =
        ai?.generateDiscussion(state, player) ?: generateFallbackChat(state, player)

    suspend fun chooseVoteTarget(state: GameState, player: Player): String? =
        ai?.chooseVoteTarget(state, player) ?: heuristicVoteTarget(state, player)

    // ── Heuristic fallbacks (used when no AI backend is configured) ──────────

    private fun heuristicNightTarget(state: GameState, player: Player): String? {
        val targets = when (player.role) {
            Role.MAFIA -> state.alivePlayers.filter { it.role?.isTown() == true }
            Role.DETECTIVE -> state.alivePlayers.filter { it.id != player.id }
            Role.DOCTOR -> state.alivePlayers
            else -> return null
        }
        if (targets.isEmpty()) return null
        return when (player.role) {
            Role.MAFIA -> {
                val accusers = findAccusers(state, player)
                if (accusers.isNotEmpty()) accusers.random().id else targets.random().id
            }
            Role.DETECTIVE -> getMostAccused(state).firstOrNull { id -> targets.any { it.id == id } } ?: targets.random().id
            Role.DOCTOR -> getMostAccused(state).firstOrNull { id -> targets.any { it.id == id } } ?: targets.random().id
            else -> null
        }
    }

    private fun heuristicVoteTarget(state: GameState, player: Player): String? {
        val targets = state.alivePlayers.filter { it.id != player.id }
        if (targets.isEmpty()) return null
        return when {
            player.role?.isMafia() == true -> {
                val townTargets = targets.filter { it.role?.isTown() == true }
                val mostAccused = getMostAccused(state)
                mostAccused.firstOrNull { id -> townTargets.any { it.id == id } }
                    ?: townTargets.randomOrNull()?.id
                    ?: targets.random().id
            }
            else -> getMostAccused(state).firstOrNull { id -> targets.any { it.id == id } } ?: targets.random().id
        }
    }

    private fun generateFallbackChat(state: GameState, player: Player): List<String> {
        val isMafia = player.role?.isMafia() == true
        val target = state.alivePlayers.filter { it.id != player.id }.randomOrNull()?.name ?: "someone"
        val phrases = if (isMafia) listOf(
            "I agree, $target has been acting strange.", "We need to be more careful with our votes.",
            "I think the Mafia is trying to frame the quiet ones.", "Let's not rush to judgment here.",
            "Something about $target doesn't sit right with me.", "I've been watching everyone closely."
        ) else listOf(
            "Something feels off about $target...", "Has anyone noticed how quiet $target has been?",
            "I think we should look at who's been deflecting.", "I'm getting a bad feeling about this round.",
            "Let's think about who benefited from last night.", "I trust my gut — $target seems suspicious."
        )
        return phrases.shuffled().take((1..2).random())
    }

    private fun findAccusers(state: GameState, mafiaPlayer: Player): List<Player> =
        state.chatMessages
            .filter { it.text.contains(mafiaPlayer.name, ignoreCase = true) }
            .mapNotNull { msg -> state.alivePlayers.find { it.id == msg.senderId } }
            .filter { it.id != mafiaPlayer.id }
            .distinct()

    private fun getMostAccused(state: GameState): List<String> {
        val mentions = mutableMapOf<String, Int>()
        state.alivePlayers.forEach { p ->
            val count = state.chatMessages.count { it.senderId != p.id && it.text.contains(p.name, ignoreCase = true) }
            if (count > 0) mentions[p.id] = count
        }
        return mentions.entries.sortedByDescending { it.value }.map { it.key }
    }
}
