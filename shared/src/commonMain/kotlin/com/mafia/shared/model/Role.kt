package com.mafia.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class Team { TOWN, MAFIA }

@Serializable
enum class Role(
    val team: Team,
    val displayName: String,
    val emoji: String,
    val description: String,
    val hasNightAction: Boolean
) {
    TOWNSFOLK(Team.TOWN, "Townsfolk", "👤", "A regular citizen. Use your wits to find the Mafia during the day.", false),
    DETECTIVE(Team.TOWN, "Detective", "🔍", "Each night, investigate one player to learn if they are Mafia.", true),
    DOCTOR(Team.TOWN, "Doctor", "💉", "Each night, choose one player to protect from elimination.", true),
    MAFIA(Team.MAFIA, "Mafia", "🔪", "Each night, choose a player to eliminate. Blend in during the day.", true);

    fun isTown(): Boolean = team == Team.TOWN
    fun isMafia(): Boolean = team == Team.MAFIA
}

object RoleDistribution {
    data class Distribution(
        val mafiaCount: Int,
        val hasDetective: Boolean,
        val hasDoctor: Boolean,
        val townsfolkCount: Int
    ) {
        fun toRoleList(): List<Role> = buildList {
            repeat(mafiaCount) { add(Role.MAFIA) }
            if (hasDetective) add(Role.DETECTIVE)
            if (hasDoctor) add(Role.DOCTOR)
            repeat(townsfolkCount) { add(Role.TOWNSFOLK) }
        }
    }

    fun forPlayerCount(count: Int): Distribution {
        require(count in 5..10) { "Player count must be between 5 and 10, got $count" }
        val mafiaCount = when (count) { in 5..6 -> 1; in 7..8 -> 2; else -> 3 }
        return Distribution(mafiaCount, true, true, count - mafiaCount - 2)
    }
}
