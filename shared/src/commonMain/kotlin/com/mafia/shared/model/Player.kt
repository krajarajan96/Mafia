package com.mafia.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    val name: String,
    val avatarEmoji: String = "👤",
    val isHost: Boolean = false,
    val isAI: Boolean = false,
    val isAlive: Boolean = true,
    val role: Role? = null,
    val isConnected: Boolean = true,
    val isSpectator: Boolean = false
) {
    fun eliminate(): Player = copy(isAlive = false)
    fun assignRole(role: Role): Player = copy(role = role)
    fun disconnect(): Player = copy(isConnected = false)
    fun reconnect(): Player = copy(isConnected = true)
}

@Serializable
data class PlayerPublicInfo(
    val id: String, val name: String, val avatarEmoji: String,
    val isHost: Boolean, val isAI: Boolean, val isAlive: Boolean, val isConnected: Boolean,
    val isSpectator: Boolean = false
) {
    companion object {
        fun from(player: Player) = PlayerPublicInfo(
            player.id, player.name, player.avatarEmoji,
            player.isHost, player.isAI, player.isAlive, player.isConnected,
            player.isSpectator
        )
    }
}

val AI_PERSONALITIES = listOf(
    Player(id = "ai_rosa", name = "Rosa", avatarEmoji = "🌹", isAI = true),
    Player(id = "ai_viktor", name = "Viktor", avatarEmoji = "🦊", isAI = true),
    Player(id = "ai_luna", name = "Luna", avatarEmoji = "🌙", isAI = true),
    Player(id = "ai_marco", name = "Marco", avatarEmoji = "🎭", isAI = true),
    Player(id = "ai_suki", name = "Suki", avatarEmoji = "🦋", isAI = true),
    Player(id = "ai_django", name = "Django", avatarEmoji = "🤠", isAI = true),
    Player(id = "ai_ivy", name = "Ivy", avatarEmoji = "🌿", isAI = true),
    Player(id = "ai_rex", name = "Rex", avatarEmoji = "🦖", isAI = true),
    Player(id = "ai_pepper", name = "Pepper", avatarEmoji = "🌶️", isAI = true),
)
