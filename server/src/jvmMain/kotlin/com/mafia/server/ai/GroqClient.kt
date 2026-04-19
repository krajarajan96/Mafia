package com.mafia.server.ai

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * Thin HTTP wrapper for the Groq OpenAI-compatible chat completions API.
 * Groq free tier: ~14,400 req/day with models like llama-3.1-70b-versatile.
 */
class GroqClient(
    private val apiKey: String,
    private val httpClient: HttpClient,
    val model: String = "llama-3.1-70b-versatile"
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Sends a chat request to Groq and returns the response text, or null on failure.
     * @param systemPrompt Sets the AI's role/context.
     * @param userPrompt The actual question or instruction.
     * @param maxTokens Limits response length (default 80 — short game decisions).
     * @param temperature Controls randomness: 0.0 = deterministic, 1.0 = creative.
     */
    suspend fun chat(
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int = 80,
        temperature: Double = 0.7
    ): String? {
        return try {
            val response = httpClient.post("https://api.groq.com/openai/v1/chat/completions") {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(JsonObject.serializer(), buildJsonObject {
                    put("model", model)
                    put("max_tokens", maxTokens)
                    put("temperature", temperature)
                    putJsonArray("messages") {
                        addJsonObject { put("role", "system"); put("content", systemPrompt) }
                        addJsonObject { put("role", "user"); put("content", userPrompt) }
                    }
                }))
            }
            val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["choices"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("message")?.jsonObject
                ?.get("content")?.jsonPrimitive?.content
                ?.trim()
        } catch (_: Exception) {
            null
        }
    }
}
