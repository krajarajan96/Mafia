package com.mafia.server.ai

import com.mafia.shared.model.*

/**
 * LLM-powered AI using Groq's fast inference (Llama 3.1 70B).
 *
 * Decision calls (night target, vote) return a player name which is fuzzy-matched
 * back to a player ID. Falls back to a random valid target if the LLM response
 * can't be matched or the API call fails.
 *
 * Discussion calls return 1-2 short in-character chat messages.
 */
class GroqGameAI(private val groq: GroqClient) : GameAI {

    override suspend fun chooseNightTarget(state: GameState, actor: Player): String? {
        val isMafia = actor.role?.isMafia() == true

        val validTargets = when (actor.role) {
            Role.MAFIA -> state.alivePlayers.filter { it.role?.isMafia() != true }
            Role.DETECTIVE, Role.VIGILANTE, Role.ESCORT -> state.alivePlayers.filter { it.id != actor.id }
            Role.DOCTOR -> state.alivePlayers
            else -> return null
        }
        if (validTargets.isEmpty()) return null

        val playerList = validTargets.joinToString(", ") { it.name }
        val mafiaTeamNames = if (isMafia)
            state.alivePlayers.filter { it.role?.isMafia() == true && it.id != actor.id }
                .joinToString(", ") { it.name }
        else ""

        val roleDesc = when (actor.role) {
            Role.MAFIA -> "a Mafia member. Your goal is to eliminate Town players and avoid detection. Do NOT target your own Mafia teammates."
            Role.DETECTIVE -> "the Detective. Investigate players to uncover Mafia members. Prioritise players who seem suspicious based on the discussion."
            Role.DOCTOR -> "the Doctor. Protect the player most likely to be targeted by the Mafia tonight."
            Role.VIGILANTE -> "the Vigilante. You can shoot one player — but if they are Town, you both die. Choose carefully."
            Role.ESCORT -> "the Escort. Block a player's night action. Target someone you suspect is Mafia or a power role."
            else -> return null
        }

        val recentChat = state.chatMessages.takeLast(6).joinToString("\n") { "${it.senderName}: ${it.text}" }

        val systemPrompt = buildString {
            append("You are ${actor.name}, ${roleDesc}")
            if (mafiaTeamNames.isNotEmpty()) append(" Your Mafia teammates are: $mafiaTeamNames.")
            append(" Round: ${state.round}.")
            append(" Players you can target tonight: $playerList.")
            if (recentChat.isNotEmpty()) append("\nRecent discussion:\n$recentChat")
            append("\n\nRespond with ONLY the exact name of the player you want to target. No explanation, no punctuation — just the name.")
        }

        val response = groq.chat(systemPrompt, "Who do you target tonight?", maxTokens = 15, temperature = 0.5)
            ?: return validTargets.random().id

        return matchPlayerName(response, validTargets)?.id ?: validTargets.random().id
    }

    override suspend fun chooseVoteTarget(state: GameState, actor: Player): String? {
        val isMafia = actor.role?.isMafia() == true
        val validTargets = if (isMafia)
            state.alivePlayers.filter { it.id != actor.id && it.role?.isMafia() != true }
        else
            state.alivePlayers.filter { it.id != actor.id }

        if (validTargets.isEmpty()) return null

        val playerList = validTargets.joinToString(", ") { it.name }
        val recentChat = state.chatMessages.takeLast(10).joinToString("\n") { "${it.senderName}: ${it.text}" }

        val systemPrompt = buildString {
            append("You are ${actor.name}, playing Mafia. Your role is ${actor.role?.displayName ?: "Town"}. Round: ${state.round}.")
            if (isMafia) append(" You are secretly Mafia — vote for a Town player to protect your team. Don't reveal yourself.")
            else append(" You are Town — vote for who you believe is Mafia based on the discussion.")
            append(" Eligible targets: $playerList.")
            if (recentChat.isNotEmpty()) append("\nDiscussion so far:\n$recentChat")
            append("\n\nRespond with ONLY the exact name of the player you vote to eliminate. No explanation, just the name.")
        }

        val response = groq.chat(systemPrompt, "Who do you vote to eliminate?", maxTokens = 15, temperature = 0.4)
            ?: return validTargets.random().id

        return matchPlayerName(response, validTargets)?.id ?: validTargets.random().id
    }

    override suspend fun generateDiscussion(state: GameState, actor: Player): List<String>? {
        val isMafia = actor.role?.isMafia() == true
        val alive = state.alivePlayers.joinToString(", ") { it.name }
        val dead = state.deadPlayers.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name } ?: "none"
        val recentChat = state.chatMessages.takeLast(10).joinToString("\n") { "${it.senderName}: ${it.text}" }

        val roleInstr = if (isMafia)
            "You are secretly Mafia. Blend in, deflect suspicion onto others, appear calm and helpful. Never reveal you are Mafia. You may subtly accuse a Town player."
        else when (actor.role) {
            Role.DETECTIVE -> "You are the Detective (Town). You have investigated someone — hint at suspicions without revealing you are the Detective."
            Role.DOCTOR -> "You are the Doctor (Town). Stay low-key — revealing your role makes you a Mafia target. Express general suspicions."
            else -> "You are Town. Discuss who seems suspicious based on their behaviour, vote patterns, and what they say. Be direct and conversational."
        }

        val systemPrompt = buildString {
            append("You are ${actor.name} ${actor.avatarEmoji} in a game of Mafia. Round ${state.round}. $roleInstr")
            append(" Players still alive: $alive.")
            if (dead != "none") append(" Eliminated: $dead.")
            if (recentChat.isNotEmpty()) append("\n\nRecent chat (respond naturally to this conversation):\n$recentChat")
            append("\n\nRules: Write 1-2 short conversational messages as yourself. Under 90 chars each. No quotation marks. No 'Name:' prefix. Sound like a real human texting in a game — casual, direct, maybe emotional. React to what others just said if relevant.")
        }

        val response = groq.chat(systemPrompt, "What do you say in the group chat?", maxTokens = 120, temperature = 0.9)
            ?: return null

        val lines = response.lines()
            .map { it.trim().removePrefix("- ").removePrefix("• ") }
            .filter { it.isNotBlank() && it.length < 150 && !it.startsWith("```") }
            .take(2)
        return lines.ifEmpty { null }
    }

    override suspend fun generateResponse(state: GameState, actor: Player, triggerMessage: ChatMessage): String? {
        // Don't reply to own messages
        if (triggerMessage.senderId == actor.id) return null
        val isMafia = actor.role?.isMafia() == true

        val roleInstr = if (isMafia)
            "You are secretly Mafia. React to what was just said — defend yourself if accused, deflect suspicion, or pretend to agree."
        else
            "You are Town (${actor.role?.displayName ?: "Townsfolk"}). React honestly to what was said — agree, disagree, or add your own take."

        val recentChat = state.chatMessages.takeLast(6).joinToString("\n") { "${it.senderName}: ${it.text}" }
        val alive = state.alivePlayers.joinToString(", ") { it.name }

        val systemPrompt = buildString {
            append("You are ${actor.name} ${actor.avatarEmoji} in a Mafia game. Round ${state.round}. $roleInstr")
            append(" Players alive: $alive.")
            if (recentChat.isNotEmpty()) append("\n\nRecent chat:\n$recentChat")
            append("\n\n${triggerMessage.senderName} just said: \"${triggerMessage.text}\"")
            append("\n\nWrite ONE short reply (under 90 chars). Casual, direct — like a real person reacting in a group chat. No quotation marks, no name prefix. May skip replying by responding with exactly: SILENT")
        }

        val response = groq.chat(systemPrompt, "Your reply:", maxTokens = 60, temperature = 0.9)
            ?: return null

        val cleaned = response.trim().removePrefix("- ").removePrefix("• ")
        if (cleaned.equals("SILENT", ignoreCase = true) || cleaned.isBlank() || cleaned.length > 150) return null
        return cleaned
    }

    /**
     * Matches a raw LLM response (player name string) to an actual player.
     * Tries exact match first, then substring containment.
     */
    private fun matchPlayerName(response: String, players: List<Player>): Player? {
        val cleaned = response.trim().lowercase().trimEnd('.', ',', '!', '?')
        players.find { it.name.lowercase() == cleaned }?.let { return it }
        players.find { cleaned.contains(it.name.lowercase()) }?.let { return it }
        players.find { it.name.lowercase().contains(cleaned) }?.let { return it }
        return null
    }
}
