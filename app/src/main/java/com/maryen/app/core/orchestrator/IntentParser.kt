package com.maryen.app.core.orchestrator

import org.json.JSONObject

data class Intent(
    val skill: String,
    val args: Map<String, Any?>,
    val confidence: Double
)

object IntentParser {
    fun parse(raw: String): Intent {
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return try {
            val o = JSONObject(cleaned)
            val args = mutableMapOf<String, Any?>()
            o.optJSONObject("args")?.let { a ->
                a.keys().forEach { k -> args[k] = a.get(k) }
            }
            Intent(
                skill = o.optString("skill", "chat"),
                args = args,
                confidence = o.optDouble("confidence", 0.5)
            )
        } catch (_: Throwable) {
            Intent("chat", emptyMap(), 0.3)
        }
    }
}
