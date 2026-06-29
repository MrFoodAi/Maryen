package com.maryen.app

import android.app.Application
import com.maryen.app.core.llm.LlmEngine
import com.maryen.app.core.memory.MemoryStore
import com.maryen.app.core.orchestrator.Orchestrator
import com.maryen.app.core.security.ConsentGate
import com.maryen.app.core.security.Vault
import com.maryen.app.skills.SkillRegistry
import com.maryen.app.skills.news.NewsSkill
import com.maryen.app.skills.recall.RecallSkill
import com.maryen.app.voice.TtsEngine

/**
 * Punto unico di assemblaggio (poor man's DI, niente Hilt/Dagger per
 * restare semplice in v1). Tutte le dipendenze condivise vivono qui.
 */
class MaryenApp : Application() {

    lateinit var vault: Vault
        private set
    lateinit var consentGate: ConsentGate
        private set
    lateinit var memoryStore: MemoryStore
        private set
    lateinit var ttsEngine: TtsEngine
        private set
    lateinit var llmEngine: LlmEngine
        private set
    lateinit var orchestrator: Orchestrator
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        vault = Vault(this)
        consentGate = ConsentGate(this)
        memoryStore = MemoryStore(this)
        ttsEngine = TtsEngine(this)
        llmEngine = LlmEngine(apiKeyProvider = { vault.getString("anthropic_api_key") })

        val skills = SkillRegistry.v1(
            news = NewsSkill(llmEngine),
            recall = RecallSkill()
        )

        orchestrator = Orchestrator(
            llm = llmEngine,
            skills = skills,
            memory = memoryStore,
            tts = ttsEngine,
            consent = consentGate
        )
    }

    companion object {
        lateinit var instance: MaryenApp
            private set
    }
}
