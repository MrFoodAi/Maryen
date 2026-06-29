package com.maryen.app.skills

import com.maryen.app.core.memory.MemoryStore
import com.maryen.app.core.orchestrator.Intent

data class SkillResult(
    val ok: Boolean,
    val spokenSummary: String,
    val artifacts: List<String> = emptyList()  // path file generati
)

interface Skill {
    val id: String
    val label: String
    val requiresConsent: Boolean
    suspend fun run(intent: Intent, memory: MemoryStore): SkillResult
}

/**
 * v1: solo skill leggere, eseguibili interamente sul telefono.
 * Le skill che richiedevano un nodo PC (image/video/film/site) sono
 * rimosse dal registro attivo. I file restano nel progetto sotto
 * skills/{image,video,film,site} come riferimento per una v2,
 * ma non sono collegati qui finché non esiste un backend reale.
 */
class SkillRegistry(private val skills: Map<String, Skill>) {
    fun resolve(id: String): Skill? = skills[id]
    fun all(): Collection<Skill> = skills.values

    companion object {
        fun v1(
            news: Skill,
            recall: Skill
        ): SkillRegistry = SkillRegistry(
            mapOf(
                news.id to news,
                recall.id to recall
            )
        )
    }
}
