package com.mafia.server.ai

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

/**
 * Thin HTTP wrapper for the Groq OpenAI-compatible chat completions API.
 * Groq free tier: ~14,400 req/day with models like llama-3.1-70b-versatile.
 */
class GroqClient(
    private val apiKey: String,
    private val httpClient: HttpClient,
    val model: String = "llama-3.3-70b-versatile"
) {
    private val log = LoggerFactory.getLogger(GroqClient::class.java)
    private val json = Json { ignoreUnknownKeys = true }

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

            val rawBody = response.bodyAsText()

            if (!response.status.isSuccess()) {
                log.warn("Groq HTTP ${response.status.value}: $rawBody")
                return null
            }

            val body = json.parseToJsonElement(rawBody).jsonObject

            // Surface API-level errors (e.g. model not found, rate limit)
            body["error"]?.jsonObject?.let { err ->
                log.warn("Groq API error: ${err["message"]?.jsonPrimitive?.content}")
                return null
            }

            val content = body["choices"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("message")?.jsonObject
                ?.get("content")?.jsonPrimitive?.content
                ?.trim()

            if (content.isNullOrBlank()) {
                log.warn("Groq returned blank content. Full body: $rawBody")
                return null
            }

            log.debug("Groq ok [${model}] → ${content.take(80)}")
            content
        } catch (e: Exception) {
            log.error("Groq request failed: ${e::class.simpleName}: ${e.message}")
            null
        }
    }
}
