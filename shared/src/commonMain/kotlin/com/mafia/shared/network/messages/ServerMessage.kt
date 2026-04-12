package com.mafia.shared.network.messages

import com.mafia.shared.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed class ServerMessage {
    @Serializable data class Error(val message: String) : ServerMessage()
    @Serializable data class RoomCreated(val room: Room, val playerId: String) : ServerMessage()
    @Serializable data class RoomJoined(val room: Room, val playerId: String) : ServerMessage()
    @Serializable data class RoomUpdate(val room: Room) : ServerMessage()
    @Serializable data class PlayerJoined(val player: PlayerPublicInfo) : ServerMessage()
    @Serializable data class PlayerLeft(val playerId: String, val playerName: String) : ServerMessage()
    @Serializable data class GameStarted(val playerCount: Int) : ServerMessage()
    @Serializable data class RoleAssigned(val role: Role) : ServerMessage()
    @Serializable data class PhaseChanged(val phase: GamePhase, val round: Int, val durationSeconds: Int, val alivePlayers: List<PlayerPublicInfo>) : ServerMessage()
    @Serializable data class NightActionAck(val success: Boolean, val message: String = "") : ServerMessage()
    @Serializable data class DetectiveResult(val targetId: String, val targetName: String, val isMafia: Boolean) : ServerMessage()
    @Serializable data class NightSummary(val eliminatedPlayer: PlayerPublicInfo?, val eliminatedRole: Role?, val wasSaved: Boolean, val vigilanteKilled: PlayerPublicInfo? = null, val vigilanteEliminated: PlayerPublicInfo? = null) : ServerMessage()
    @Serializable data class ChatReceived(val message: ChatMessage) : ServerMessage()
    @Serializable data class VoteUpdate(val voterId: String, val targetId: String, val currentTally: Map<String, Int>, val currentSkips: Set<String> = emptySet()) : ServerMessage()
    @Serializable data class VoteResult(val eliminatedPlayer: PlayerPublicInfo?, val eliminatedRole: Role?, val wasTie: Boolean, val finalTally: Map<String, Int>) : ServerMessage()
    @Serializable data class GameOver(val winner: Team, val allRoles: Map<String, Role>, val mvp: String? = null) : ServerMessage()
    @Serializable data class TimerTick(val secondsRemaining: Int) : ServerMessage()
    @Serializable data class SettingsUpdated(val settings: GameSettings) : ServerMessage()

    companion object {
        private val json = Json { ignoreUnknownKeys = true; classDiscriminator = "type" }
        fun encode(msg: ServerMessage): String = json.encodeToString(serializer(), msg)
        fun decode(raw: String): ServerMessage = json.decodeFromString(serializer(), raw)
    }
}
