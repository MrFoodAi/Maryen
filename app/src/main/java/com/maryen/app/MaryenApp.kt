package com.maryen.app

import android.app.Application
import com.maryen.app.core.llm.LlmEngine
import com.maryen.app.core.memory.MemoryStore
import com.maryen.app.core.orchestrator.Orchestrator
import com.maryen.app.core.security.ConsentGate
import com.maryen.app.core.security.Vault
import com.maryen.app.core.settings.PreferencesStore
import com.maryen.app.skills.SkillRegistry
import com.maryen.app.skills.news.NewsSkill
import com.maryen.app.skills.recall.RecallSkill
import com.maryen.app.voice.TtsEngine

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
    lateinit var preferencesStore: PreferencesStore
        private set

    private lateinit var skillRegistry: SkillRegistry

    override fun onCreate() {
        super.onCreate()
        instance = this

        vault = Vault(this)
        consentGate = ConsentGate(this)
        memoryStore = MemoryStore(this)
        ttsEngine = TtsEngine(this)
        preferencesStore = PreferencesStore(this)

        llmEngine = LlmEngine(
            apiKeyProvider = { vault.getString("groq_api_key") },
            modelProvider = { preferencesStore.getModel().apiId }
        )

        skillRegistry = SkillRegistry.v1(
            news = NewsSkill(llmEngine),
            recall = RecallSkill()
        )

        orchestrator = Orchestrator(
            llm = llmEngine,
            skills = skillRegistry,
            memory = memoryStore,
            tts = ttsEngine,
            consent = consentGate,
            preferences = preferencesStore
        )
    }

    fun orchestratorSkills(): SkillRegistry = skillRegistry

    companion object {
        lateinit var instance: MaryenApp
            private set
    }
}
