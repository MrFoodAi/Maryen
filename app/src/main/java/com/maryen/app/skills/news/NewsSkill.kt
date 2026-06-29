package com.maryen.app.skills.news

import com.maryen.app.core.llm.LlmEngine
import com.maryen.app.core.memory.MemoryStore
import com.maryen.app.core.orchestrator.Intent
import com.maryen.app.skills.Skill
import com.maryen.app.skills.SkillResult

/**
 * Skill notizie v1: chiede direttamente al modello un riassunto.
 * Nota: l'LLM non ha accesso internet proprio (l'API Claude di base
 * non naviga il web), quindi questa v1 produce un riassunto basato
 * sulla conoscenza del modello, non notizie in tempo reale.
 * Per notizie davvero aggiornate andrebbe integrata una vera fonte
 * (RSS, NewsAPI) — rimandato a v2 per restare nello scope minimo.
 */
class NewsSkill(private val llm: LlmEngine) : Skill {
    override val id = "news"
    override val label = "Notizie"
    override val requiresConsent = false

    override suspend fun run(intent: Intent, memory: MemoryStore): SkillResult {
        val topic = intent.args["topic"]?.toString() ?: "attualità generale"
        val text = llm.completeJson(
            system = "Rispondi in italiano con un breve riassunto in massimo 5 punti, " +
                "informando chiaramente l'utente che la tua conoscenza ha un limite temporale " +
                "e che per notizie dell'ultim'ora serve una fonte live.",
            user = "Dammi un riassunto su: $topic"
        )
        return SkillResult(ok = true, spokenSummary = text)
    }
}
