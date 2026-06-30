package com.maryen.app.core.orchestrator

import com.maryen.app.core.llm.LlmEngine
import com.maryen.app.core.memory.MemoryStore
import com.maryen.app.core.security.ConsentGate
import com.maryen.app.core.settings.PreferencesStore
import com.maryen.app.skills.SkillRegistry
import com.maryen.app.voice.TtsEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class Orchestrator(
    private val llm: LlmEngine,
    private val skills: SkillRegistry,
    private val memory: MemoryStore,
    private val tts: TtsEngine,
    private val consent: ConsentGate,
    private val preferences: PreferencesStore
) {

    fun handle(userText: String, speakOut: Boolean = true): Flow<String> = flow {
        try {
            val recent = memory.recent(limit = 8)
            val rag = memory.semanticSearch(userText, k = 4)

            val intentJson = try {
                llm.completeJson(
                    system = SYSTEM_INTENT,
                    user = buildIntentPrompt(userText, recent, rag)
                )
            } catch (e: Exception) {
                ""
            }
            val intent = IntentParser.parse(intentJson)

            val skill = skills.resolve(intent.skill)
            if (skill == null) {
                val full = StringBuilder()
                llm.stream(
                    system = preferences.buildSystemPrompt(),
                    user = buildChatPrompt(userText, recent, rag)
                ).collect { tok ->
                    full.append(tok)
                    emit(tok)
                }
                val answer = full.toString().trim().ifBlank {
                    "Non sono riuscita a generare una risposta. Riprova."
                }
                memory.write(MemoryStore.Turn(userText, answer, null))
                if (speakOut) tts.speak(answer)
                return@flow
            }

            if (skill.requiresConsent && !consent.granted(skill.id)) {
                val msg = "Per eseguire «${skill.label}» mi serve la tua conferma. Vai in Impostazioni > Consensi."
                emit(msg)
                memory.write(MemoryStore.Turn(userText, msg, skill.id))
                if (speakOut) tts.speak(msg)
                return@flow
            }

            val result = skill.run(intent, memory)
            val rendered = result.spokenSummary
            emit(rendered)
            memory.write(MemoryStore.Turn(userText, rendered, skill.id))
            if (speakOut) tts.speak(rendered)

        } catch (e: Exception) {
            val msg = "Ops, qualcosa è andato storto: ${e.message ?: "errore sconosciuto"}. Riprova."
            emit(msg)
            try { memory.write(MemoryStore.Turn(userText, msg, null)) } catch (_: Exception) {}
            if (speakOut) try { tts.speak(msg) } catch (_: Exception) {}
        }
    }

    companion object {
        private val SYSTEM_INTENT = """
            Sei il parser di intent di Maryen. Rispondi SOLO con JSON valido, niente altro testo:
            {"skill":"<id>", "args":{...}, "confidence":0.0-1.0}
            Skill disponibili: chat, news, memory_recall.
            Se la richiesta riguarda generare immagini, video o altro non elencato,
            usa comunque skill="chat" (Maryen risponderà spiegando il limite).
            Se non sei sicuro o la richiesta è una conversazione generica usa "chat".
        """.trimIndent()

        private fun buildIntentPrompt(text: String, recent: List<String>, rag: List<String>): String =
            "Contesto recente:\n${recent.joinToString("\n")}\n\nRichiamo memoria:\n${rag.joinToString("\n")}\n\nUtente: $text"

        private fun buildChatPrompt(text: String, recent: List<String>, rag: List<String>): String =
            "Memoria rilevante:\n${rag.joinToString("\n")}\n\nUltimi turni:\n${recent.joinToString("\n")}\n\nEnrico: $text\nMaryen:"
    }
}
