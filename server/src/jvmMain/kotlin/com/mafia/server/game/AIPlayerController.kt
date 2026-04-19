package com.mafia.server.game

import com.mafia.server.ai.GameAI
import com.mafia.shared.model.*
import org.slf4j.LoggerFactory

/**
 * Coordinates AI player actions within a game session.
 * All decision logic is delegated to the injected [GameAI] implementation
 * (e.g. GroqGameAI), with a simple heuristic fallback when no AI is configured.
 */
class AIPlayerController(private val ai: GameAI? = null) {

    private val log = LoggerFactory.getLogger(AIPlayerController::class.java)

    suspend fun chooseNightTarget(state: GameState, player: Player): String? =
        ai?.chooseNightTarget(state, player) ?: heuristicNightTarget(state, player)

    suspend fun generateDiscussion(state: GameState, player: Player): List<String> {
        val result = ai?.generateDiscussion(state, player)
        if (result == null && ai != null) log.info("Groq discussion fallback for ${player.name} (${player.role})")
        return result ?: generateFallbackChat(state, player)
    }

    suspend fun chooseVoteTarget(state: GameState, player: Player): String? =
        ai?.chooseVoteTarget(state, player) ?: heuristicVoteTarget(state, player)

    suspend fun generateResponse(state: GameState, player: Player, triggerMessage: ChatMessage): String? {
        val result = ai?.generateResponse(state, player, triggerMessage)
        if (result == null && ai != null) log.info("Groq response fallback for ${player.name}")
        return result ?: heuristicResponse(state, player, triggerMessage)
    }

    suspend fun generateNightReaction(state: GameState, player: Player, eliminatedName: String): String? {
        val result = ai?.generateNightReaction(state, player, eliminatedName)
        return result ?: heuristicNightReaction(player, eliminatedName)
    }

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

    private fun heuristicNightReaction(player: Player, eliminatedName: String): String? {
        val isMafia = player.role?.isMafia() == true
        val phrases = if (isMafia) listOf(
            "Oh no... $eliminatedName is gone",
            "This is getting intense",
            "We need to find the Mafia fast",
            "Poor $eliminatedName... we failed them"
        ) else listOf(
            "oh wow, $eliminatedName!! I suspected them honestly",
            "rip $eliminatedName... now who do we vote?",
            "did NOT see that coming with $eliminatedName",
            "Mafia got $eliminatedName... we need to focus",
            "I had a feeling about $eliminatedName ngl"
        )
        return phrases.random()
    }

    private fun heuristicResponse(state: GameState, player: Player, trigger: ChatMessage): String? {
        val isMafia = player.role?.isMafia() == true
        val senderName = trigger.senderName
        val isAccused = trigger.text.contains(player.name, ignoreCase = true)
        // Always reply if accused; otherwise 70% chance
        if (!isAccused && (0..9).random() < 3) return null
        val phrases = if (isAccused) listOf(
            "Woah why are you pointing at me??",
            "That's not fair, I'm clearly Town",
            "Classic deflection move right there",
            "I'm not the one you should worry about",
            "Really? Me? Look at the others first"
        ) else if (isMafia) listOf(
            "I don't think it's $senderName tbh",
            "We shouldn't trust $senderName blindly",
            "Something about $senderName feels off too",
            "Let's focus on the quieter ones",
            "I'm not convinced by $senderName's logic"
        ) else listOf(
            "Yeah I was thinking the same",
            "$senderName has a point actually",
            "I don't know, something feels off here",
            "Who else has been quiet this round?",
            "That tracks with what I noticed too"
        )
        return phrases.random()
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
