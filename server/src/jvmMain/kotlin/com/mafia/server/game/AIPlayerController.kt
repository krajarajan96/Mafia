package com.mafia.server.game

import com.mafia.shared.model.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

class AIPlayerController(
    private val apiKey: String = System.getenv("ANTHROPIC_API_KEY") ?: "",
    private val useClaudeForChat: Boolean = true
) {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun chooseNightTarget(state: GameState, player: Player): String? {
        val targets = when (player.role) {
            Role.MAFIA -> state.alivePlayers.filter { it.role?.isTown() == true }
            Role.DETECTIVE -> state.alivePlayers.filter { it.id != player.id }
            Role.DOCTOR -> state.alivePlayers
            else -> return null
        }
        if (targets.isEmpty()) return null
        return when (player.role) {
            Role.MAFIA -> { val accusers = findAccusers(state, player); if (accusers.isNotEmpty()) accusers.random().id else targets.random().id }
            Role.DETECTIVE -> { val suspicious = getMostAccused(state); suspicious.firstOrNull { id -> targets.any { it.id == id } } ?: targets.random().id }
            Role.DOCTOR -> { val atRisk = getMostAccused(state); atRisk.firstOrNull { id -> targets.any { it.id == id } } ?: targets.random().id }
            else -> null
        }
    }

    suspend fun generateDiscussion(state: GameState, player: Player): List<String> {
        if (!useClaudeForChat || apiKey.isBlank()) return generateFallbackChat(state, player)
        return try {
            val prompt = buildDiscussionPrompt(state, player)
            parseMessages(callClaude(prompt))
        } catch (_: Exception) { generateFallbackChat(state, player) }
    }

    suspend fun chooseVoteTarget(state: GameState, player: Player): String? {
        val targets = state.alivePlayers.filter { it.id != player.id }
        if (targets.isEmpty()) return null
        return when {
            player.role?.isMafia() == true -> {
                val townTargets = targets.filter { it.role?.isTown() == true }
                val mostAccused = getMostAccused(state)
                mostAccused.firstOrNull { id -> townTargets.any { it.id == id } } ?: townTargets.randomOrNull()?.id ?: targets.random().id
            }
            else -> { val mostAccused = getMostAccused(state); mostAccused.firstOrNull { id -> targets.any { it.id == id } } ?: targets.random().id }
        }
    }

    private fun findAccusers(state: GameState, mafiaPlayer: Player): List<Player> =
        state.chatMessages.filter { it.text.contains(mafiaPlayer.name, ignoreCase = true) }
            .mapNotNull { msg -> state.alivePlayers.find { it.id == msg.senderId } }
            .filter { it.id != mafiaPlayer.id }.distinct()

    private fun getMostAccused(state: GameState): List<String> {
        val mentions = mutableMapOf<String, Int>()
        state.alivePlayers.forEach { p ->
            val count = state.chatMessages.count { it.senderId != p.id && it.text.contains(p.name, ignoreCase = true) }
            if (count > 0) mentions[p.id] = count
        }
        return mentions.entries.sortedByDescending { it.value }.map { it.key }
    }

    private fun buildDiscussionPrompt(state: GameState, player: Player): String {
        val role = player.role ?: Role.TOWNSFOLK
        val recentChat = state.chatMessages.takeLast(10).joinToString("\n") { "${it.senderName}: ${it.text}" }
        val alive = state.alivePlayers.joinToString(", ") { it.name }
        val dead = state.deadPlayers.joinToString(", ") { "${it.name} (${it.role?.displayName ?: "?"})" }
        val roleInstr = if (role.isMafia()) "You are MAFIA. Blend in and deflect suspicion. Never reveal you are Mafia."
            else "You are TOWN (${role.displayName}). Try to find the Mafia through reasoning."
        return "You are ${player.name} ${player.avatarEmoji}, playing Mafia. Role: ${role.displayName}.\nRound: ${state.round}. Alive: $alive. Dead: ${dead.ifEmpty { "none" }}.\n$roleInstr\nRecent chat:\n$recentChat\nRespond with 1-3 short chat messages (one per line). Keep each under 100 chars. No name prefix."
    }

    private suspend fun callClaude(prompt: String): String {
        val response = client.post("https://api.anthropic.com/v1/messages") {
            header("x-api-key", apiKey); header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(JsonObject.serializer(), buildJsonObject {
                put("model", "claude-sonnet-4-20250514"); put("max_tokens", 300)
                putJsonArray("messages") { addJsonObject { put("role", "user"); put("content", prompt) } }
            }))
        }
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        return body["content"]?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
    }

    private fun parseMessages(response: String) = response.lines().map { it.trim() }.filter { it.isNotBlank() && it.length < 150 }.take(3)

    private fun generateFallbackChat(state: GameState, player: Player): List<String> {
        val isMafia = player.role?.isMafia() == true
        val target = state.alivePlayers.filter { it.id != player.id }.randomOrNull()?.name ?: "someone"
        val phrases = if (isMafia) listOf(
            "I agree, $target has been acting strange.", "We need to be more careful with our votes.",
            "I think the Mafia is trying to frame the quiet ones.", "Let's not rush to judgment here.",
            "Something about $target doesn't sit right with me.", "I've been watching everyone closely."
        ) else listOf(
            "Something feels off about $target...", "Has anyone noticed how quiet $target has been?",
            "I think we should look at who's been deflecting.", "I'm getting a bad feeling about this round.",
            "Let's think about who benefited from last night.", "I trust my gut — $target seems suspicious."
        )
        return phrases.shuffled().take((1..2).random())
    }
}
