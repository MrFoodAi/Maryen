package com.maryen.app.core.settings

import android.content.Context

class PreferencesStore(ctx: Context) {

    private val prefs = ctx.getSharedPreferences("maryen_prefs", Context.MODE_PRIVATE)

    enum class GroqModel(val apiId: String, val label: String) {
        FAST("llama-3.1-8b-instant", "Rapido (8B)"),
        SMART("llama-3.3-70b-versatile", "Più capace (70B)")
    }

    enum class Tone(val label: String, val promptFragment: String) {
        INFORMAL("Informale", "calda, diretta, informale, senza fronzoli"),
        FORMAL("Formale", "professionale, educata, formale"),
        PLAYFUL("Scherzosa", "simpatica, scherzosa, con un pizzico di ironia")
    }

    fun getModel(): GroqModel {
        val saved = prefs.getString("model", GroqModel.FAST.name)
        return GroqModel.entries.find { it.name == saved } ?: GroqModel.FAST
    }

    fun setModel(model: GroqModel) {
        prefs.edit().putString("model", model.name).apply()
    }

    fun getTone(): Tone {
        val saved = prefs.getString("tone", Tone.INFORMAL.name)
        return Tone.entries.find { it.name == saved } ?: Tone.INFORMAL
    }

    fun setTone(tone: Tone) {
        prefs.edit().putString("tone", tone.name).apply()
    }

    fun getCustomPersonality(): String =
        prefs.getString("custom_personality", "") ?: ""

    fun setCustomPersonality(text: String) {
        prefs.edit().putString("custom_personality", text).apply()
    }

    fun buildSystemPrompt(): String {
        val tone = getTone()
        val custom = getCustomPersonality()
        val base = "Sei Maryen, assistente personale di Enrico, ${tone.promptFragment}. " +
            "Rispondi in italiano, massimo 4 frasi se la domanda è semplice, più lunga se serve."
        return if (custom.isNotBlank()) "$base\n\nIstruzioni aggiuntive: $custom" else base
    }
}
