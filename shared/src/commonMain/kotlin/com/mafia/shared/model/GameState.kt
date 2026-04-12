package com.mafia.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val roomId: String,
    val phase: GamePhase = GamePhase.LOBBY,
    val round: Int = 0,
    val players: List<Player> = emptyList(),
    val phaseTimeRemaining: Int = 0,
    val nightActions: NightActions = NightActions(),
    val votes: Map<String, String> = emptyMap(),
    val skips: Set<String> = emptySet(),
    val eliminatedThisRound: String? = null,
    val vigilanteEliminated: String? = null,
    val savedThisNight: Boolean = false,
    val ministerVetoUsed: Boolean = false,
    val ministerVetoThisRound: Boolean = false,
    val winner: Team? = null,
    val chatMessages: List<ChatMessage> = emptyList()
) {
    val alivePlayers get() = players.filter { it.isAlive }
    val deadPlayers get() = players.filter { !it.isAlive }
    val aliveTown get() = alivePlayers.filter { it.role?.isTown() == true }
    val aliveMafia get() = alivePlayers.filter { it.role?.isMafia() == true }

    fun checkWinCondition(): Team? = when {
        aliveMafia.isEmpty() -> Team.TOWN
        aliveMafia.size >= aliveTown.size -> Team.MAFIA
        else -> null
    }

    fun getPlayer(id: String) = players.find { it.id == id }

    fun updatePlayer(id: String, transform: (Player) -> Player) =
        copy(players = players.map { if (it.id == id) transform(it) else it })

    fun voteResult(): String? {
        if (votes.isEmpty()) return null
        val tally = votes.values.groupingBy { it }.eachCount()
        val maxVotes = tally.maxOf { it.value }
        val top = tally.filter { it.value == maxVotes }.keys
        return if (top.size == 1) top.first() else null
    }
}

@Serializable
data class NightActions(
    val mafiaTarget: String? = null,
    val doctorProtect: String? = null,
    val detectiveInvestigate: String? = null,
    val vigilanteTarget: String? = null,
    val escortBlock: String? = null
) {
    fun resolve(): NightResolution {
        // Escort block nullifies the blocked player's action
        val effectiveMafiaTarget = if (mafiaTarget != null && mafiaTarget == escortBlock) null else mafiaTarget
        val effectiveVigilanteTarget = if (vigilanteTarget != null && vigilanteTarget == escortBlock) null else vigilanteTarget
        val effectiveDetective = if (detectiveInvestigate != null && detectiveInvestigate == escortBlock) null else detectiveInvestigate
        val effectiveDoctor = if (doctorProtect != null && doctorProtect == escortBlock) null else doctorProtect

        val mafiaKilled = effectiveMafiaTarget != null && effectiveMafiaTarget != effectiveDoctor
        val wasSaved = effectiveMafiaTarget != null && effectiveMafiaTarget == effectiveDoctor

        return NightResolution(
            eliminatedId = if (mafiaKilled) effectiveMafiaTarget else null,
            wasSaved = wasSaved,
            investigatedId = effectiveDetective,
            vigilanteTargetId = effectiveVigilanteTarget
        )
    }
}

@Serializable
data class NightResolution(
    val eliminatedId: String?,
    val wasSaved: Boolean,
    val investigatedId: String?,
    val vigilanteTargetId: String? = null
)

@Serializable
data class ChatMessage(
    val id: String, val senderId: String, val senderName: String,
    val text: String, val timestamp: Long, val isSystem: Boolean = false
)

/** A single entry in the voting log shown to all players. */
data class VoteEntry(val voterName: String, val targetName: String, val isSkip: Boolean)

/** An entry in the game event history shown in the Arena tab. */
data class GameEvent(
    val round: Int,
    val label: String,
    val description: String,
    val isElimination: Boolean = false
)
