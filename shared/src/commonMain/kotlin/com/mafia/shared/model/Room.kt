package com.mafia.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Room(
    val id: String, val code: String, val hostId: String, val mode: GameMode,
    val maxPlayers: Int = 8, val minPlayers: Int = 5,
    val players: List<PlayerPublicInfo> = emptyList(),
    val status: RoomStatus = RoomStatus.WAITING,
    val settings: GameSettings = GameSettings()
) {
    val playerCount get() = players.size
    val isFull get() = playerCount >= maxPlayers
    val canStart get() = playerCount >= minPlayers || mode == GameMode.SINGLE_PLAYER
}

@Serializable enum class GameMode { SINGLE_PLAYER, MULTIPLAYER }
@Serializable enum class RoomStatus { WAITING, IN_GAME, FINISHED }

@Serializable
data class GameSettings(
    val discussionTimeSec: Int = 90, val votingTimeSec: Int = 30,
    val nightTimeSec: Int = 30, val allowAIFill: Boolean = true,
    val revealRoleOnDeath: Boolean = true, val anonymousVoting: Boolean = false
)

fun generateRoomCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..6).map { chars.random() }.joinToString("")
}
