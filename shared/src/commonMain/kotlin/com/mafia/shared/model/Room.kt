package com.mafia.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Room(
    val id: String, val code: String, val hostId: String, val mode: GameMode,
    val maxPlayers: Int = 8, val minPlayers: Int = 1,
    val players: List<PlayerPublicInfo> = emptyList(),
    val spectators: List<PlayerPublicInfo> = emptyList(),
    val status: RoomStatus = RoomStatus.WAITING,
    val settings: GameSettings = GameSettings()
) {
    val playerCount get() = players.size
    val isFull get() = playerCount >= maxPlayers
    val canStart get() = playerCount >= 1
}

@Serializable enum class GameMode { SINGLE_PLAYER, MULTIPLAYER }
@Serializable enum class RoomStatus { WAITING, IN_GAME, FINISHED }

@Serializable
data class GameSettings(
    val discussionTimeSec: Int = 60, val votingTimeSec: Int = 30,
    val nightTimeSec: Int = 30, val allowAIFill: Boolean = true,
    val revealRoleOnDeath: Boolean = false, val anonymousVoting: Boolean = false,
    /** Total players in the game including AI fill. Range: 5–10. */
    val maxPlayers: Int = 8,
    // Doctor is always on (base/classic role).
    // All others are optional specials — off by default (classic preset).
    val enableDoctor: Boolean = true,
    val enableDetective: Boolean = false,
    val enableVigilante: Boolean = false,
    val enableEscort: Boolean = false,
    val enableMinister: Boolean = false,
    val enableGameHistory: Boolean = true,
    val enableTips: Boolean = true
)

fun generateRoomCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..6).map { chars.random() }.joinToString("")
}
