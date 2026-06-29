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

/**
 * Motore LLM v1: chiamata API remota (Anthropic Claude), NON modello locale.
 * Niente JNI, niente NDK, niente file .gguf da 4+ GB: questo gira su
 * qualunque telefono e si compila senza toolchain nativa.
 *
 * La chiave API si inserisce dalle Impostazioni dell'app ed è salvata
 * cifrata nel Vault — non è hardcoded qui.
 */
class LlmEngine(private val apiKeyProvider: () -> String?) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val endpoint = "https://api.anthropic.com/v1/messages"

    private fun requireKey(): String =
        apiKeyProvider() ?: throw IllegalStateException(
            "Nessuna API key configurata. Vai in Impostazioni > Maryen e inseriscila."
        )

    /** Risposta unica (non streaming), usata per il parsing dell'intent JSON. */
    suspend fun completeJson(system: String, user: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("model", "claude-sonnet-4-6")
            put("max_tokens", 512)
            put("system", system)
            put("messages", JSONArray().put(
                JSONObject().put("role", "user").put("content", user)
            ))
        }
        val resp = call(body)
        extractText(resp)
    }

    /** Risposta in streaming a token, usata per la chat libera. */
    fun stream(system: String, user: String): Flow<String> = flow {
        // v1: niente streaming SSE reale (più complesso da gestire su Android
        // senza librerie aggiuntive). Si chiama l'API in modo sincrono e si
        // emette il risultato come unico "token" — l'interfaccia resta identica,
        // quindi passare a streaming vero in futuro non rompe l'Orchestrator.
        val text = withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("model", "claude-sonnet-4-6")
                put("max_tokens", 1024)
                put("system", system)
                put("messages", JSONArray().put(
                    JSONObject().put("role", "user").put("content", user)
                ))
            }
            extractText(call(body))
        }
        emit(text)
    }

    private fun call(body: JSONObject): JSONObject {
        val req = Request.Builder()
            .url(endpoint)
            .addHeader("x-api-key", requireKey())
            .addHeader("anthropic-version", "2023-06-01")
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
        val content = resp.optJSONArray("content") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until content.length()) {
            val block = content.getJSONObject(i)
            if (block.optString("type") == "text") sb.append(block.optString("text"))
        }
        return sb.toString().trim()
    }
}
