package com.mafia.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class GamePhase(val displayName: String, val defaultDurationSeconds: Int, val description: String) {
    LOBBY("Lobby", 0, "Waiting for players to join"),
    ROLE_REVEAL("Role Reveal", 8, "Discover your secret identity"),
    NIGHT("Night", 30, "The town sleeps... Mafia, choose your target"),
    NIGHT_RESULT("Dawn", 6, "The sun rises and the town discovers what happened"),
    DISCUSSION("Discussion", 90, "Discuss, accuse, and defend — find the Mafia!"),
    VOTING("Voting", 30, "Vote to eliminate a suspect"),
    ELIMINATION("Elimination", 6, "The town has decided..."),
    GAME_OVER("Game Over", 0, "The game has ended");

    fun isNightPhase(): Boolean = this == NIGHT || this == NIGHT_RESULT
    fun isDayPhase(): Boolean = this == DISCUSSION || this == VOTING || this == ELIMINATION
    fun isActive(): Boolean = this != LOBBY && this != GAME_OVER

    fun next(): GamePhase = when (this) {
        LOBBY -> ROLE_REVEAL; ROLE_REVEAL -> NIGHT; NIGHT -> NIGHT_RESULT
        NIGHT_RESULT -> DISCUSSION; DISCUSSION -> VOTING; VOTING -> ELIMINATION
        ELIMINATION -> NIGHT; GAME_OVER -> GAME_OVER
    }
}
