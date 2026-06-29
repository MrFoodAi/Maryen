package com.maryen.app.skills.recall

import com.maryen.app.core.memory.MemoryStore
import com.maryen.app.core.orchestrator.Intent
import com.maryen.app.skills.Skill
import com.maryen.app.skills.SkillResult

/** Skill richiamo memoria: restituisce gli ultimi scambi salvati. */
class RecallSkill : Skill {
    override val id = "memory_recall"
    override val label = "Richiamo memoria"
    override val requiresConsent = false

    override suspend fun run(intent: Intent, memory: MemoryStore): SkillResult {
        val items = memory.recent(limit = 10)
        val summary = if (items.isEmpty()) {
            "Non ho ancora nulla in memoria con te."
        } else {
            "Ecco cosa ricordo:\n" + items.joinToString("\n")
        }
        return SkillResult(ok = true, spokenSummary = summary)
    }
}
