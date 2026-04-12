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
    VIGILANTE(Team.TOWN, "Vigilante", "🤠", "Once per game, shoot a player at night. If your target is innocent, you die too.", true),
    ESCORT(Team.TOWN, "Escort", "💃", "Each night, block one player's night action.", true),
    MINISTER(Team.TOWN, "Minister", "🏛️", "Once per game, secretly veto an elimination vote. Your identity stays hidden.", false),
    MAFIA(Team.MAFIA, "Mafia", "🔪", "Each night, choose a player to eliminate. Blend in during the day.", true);

    fun isTown(): Boolean = team == Team.TOWN
    fun isMafia(): Boolean = team == Team.MAFIA
}

object RoleDistribution {
    fun forPlayerCount(count: Int, settings: GameSettings = GameSettings()): List<Role> {
        require(count in 5..10) { "Player count must be between 5 and 10, got $count" }
        val mafiaCount = when (count) { in 5..6 -> 1; in 7..8 -> 2; else -> 3 }
        val specials = buildList {
            if (settings.enableDetective) add(Role.DETECTIVE)
            if (settings.enableDoctor) add(Role.DOCTOR)
            if (settings.enableVigilante) add(Role.VIGILANTE)
            if (settings.enableEscort) add(Role.ESCORT)
            if (settings.enableMinister) add(Role.MINISTER)
        }
        // cap specials so there's at least 1 townsfolk
        val maxSpecials = count - mafiaCount - 1
        val usedSpecials = specials.take(maxSpecials)
        val townsfolkCount = count - mafiaCount - usedSpecials.size
        return buildList {
            repeat(mafiaCount) { add(Role.MAFIA) }
            addAll(usedSpecials)
            repeat(townsfolkCount) { add(Role.TOWNSFOLK) }
        }
    }
}
