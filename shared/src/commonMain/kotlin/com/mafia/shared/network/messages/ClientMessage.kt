package com.mafia.shared.network.messages

import com.mafia.shared.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed class ClientMessage {
    @Serializable data class CreateRoom(val mode: GameMode, val playerName: String, val emoji: String = "🕵️") : ClientMessage()
    @Serializable data class JoinRoom(val roomCode: String, val playerName: String, val emoji: String = "🕵️") : ClientMessage()
    @Serializable data object StartGame : ClientMessage()
    @Serializable data class NightAction(val targetId: String) : ClientMessage()
    @Serializable data class SendChat(val text: String) : ClientMessage()
    @Serializable data class CastVote(val targetId: String) : ClientMessage()
    @Serializable data class Accuse(val targetId: String, val reason: String) : ClientMessage()
    @Serializable data object SkipVote : ClientMessage()
    @Serializable data object UseVeto : ClientMessage()
    @Serializable data class UpdateSettings(val settings: GameSettings) : ClientMessage()
    @Serializable data class MafiaVote(val targetId: String) : ClientMessage()
    @Serializable data class MafiaChat(val text: String) : ClientMessage()
    @Serializable data object LeaveRoom : ClientMessage()
    @Serializable data object Ready : ClientMessage()

    companion object {
        private val json = Json { ignoreUnknownKeys = true; classDiscriminator = "type" }
        fun encode(msg: ClientMessage): String = json.encodeToString(serializer(), msg)
        fun decode(raw: String): ClientMessage = json.decodeFromString(serializer(), raw)
    }
}
