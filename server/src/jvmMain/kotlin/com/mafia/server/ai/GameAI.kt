package com.mafia.server.ai

import com.mafia.shared.model.GameState
import com.mafia.shared.model.Player

/**
 * Abstraction layer for AI decision-making in the Mafia game.
 * Implementations can use any LLM backend (Groq, Claude, etc.) or heuristics.
 */
interface GameAI {
    /** Returns the ID of the player to target during the night phase, or null if no action. */
    suspend fun chooseNightTarget(state: GameState, actor: Player): String?

    /** Returns the ID of the player to vote for during the voting phase, or null to skip. */
    suspend fun chooseVoteTarget(state: GameState, actor: Player): String?

    /** Returns a list of chat messages to send during the discussion phase. */
    suspend fun generateDiscussion(state: GameState, actor: Player): List<String>
}
