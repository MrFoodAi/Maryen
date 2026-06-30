package com.maryen.app.core.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LlmEngine(
    private val apiKeyProvider: () -> String?,
    private val modelProvider: () -> String = { "llama-3.1-8b-instant" }
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val endpoint = "https://api.groq.com/openai/v1/chat/completions"

    private fun requireKey(): String =
        apiKeyProvider() ?: throw IllegalStateException(
            "Nessuna API key configurata. Vai in Impostazioni > Maryen e inseriscila."
        )

    suspend fun testKey(): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("model", modelProvider())
                put("max_tokens", 5)
                put("messages", JSONArray().put(
                    JSONObject().put("role", "user").put("content", "ciao")
                ))
            }
            call(body)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun completeJson(system: String, user: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("model", modelProvider())
            put("max_tokens", 512)
            put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", system))
                .put(JSONObject().put("role", "user").put("content", user))
            )
        }
        val resp = call(body)
        extractText(resp)
    }

    fun stream(system: String, user: String): Flow<String> = flow {
        val text = withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("model", modelProvider())
                put("max_tokens", 1024)
                put("messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", system))
                    .put(JSONObject().put("role", "user").put("content", user))
                )
            }
            extractText(call(body))
        }
        emit(text)
    }

    private fun call(body: JSONObject): JSONObject {
        val req = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer ${requireKey()}")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("Errore API (${resp.code}): $raw")
            }
            return JSONObject(raw)
        }
    }

    private fun extractText(resp: JSONObject): String {
        val choices = resp.optJSONArray("choices") ?: return ""
        if (choices.length() == 0) return ""
        val message = choices.getJSONObject(0).optJSONObject("message") ?: return ""
        return message.optString("content", "").trim()
    }
}
