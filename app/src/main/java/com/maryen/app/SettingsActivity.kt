package com.maryen.app

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.maryen.app.core.settings.PreferencesStore
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val apiKeyField = findViewById<EditText>(R.id.apiKeyField)
        val saveButton = findViewById<Button>(R.id.saveApiKeyButton)
        val testButton = findViewById<Button>(R.id.testApiKeyButton)
        val statusText = findViewById<TextView>(R.id.apiKeyStatus)
        val modelSpinner = findViewById<Spinner>(R.id.modelSpinner)
        val toneSpinner = findViewById<Spinner>(R.id.toneSpinner)
        val personalityField = findViewById<EditText>(R.id.personalityField)
        val savePrefsButton = findViewById<Button>(R.id.savePreferencesButton)
        val clearMemoryButton = findViewById<Button>(R.id.clearMemoryButton)

        val vault = MaryenApp.instance.vault
        val prefs = MaryenApp.instance.preferencesStore

        vault.getString("groq_api_key")?.let { apiKeyField.setText(it) }

        saveButton.setOnClickListener {
            val key = apiKeyField.text.toString().trim()
            if (key.isNotEmpty()) {
                vault.putString("groq_api_key", key)
                Toast.makeText(this, "API key salvata", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Inserisci una API key valida", Toast.LENGTH_SHORT).show()
            }
        }

        testButton.setOnClickListener {
            statusText.text = "Verifica…"
            lifecycleScope.launch {
                val ok = MaryenApp.instance.llmEngine.testKey()
                statusText.text = if (ok) "✓ valida" else "✗ non valida"
            }
        }

        val modelOptions = PreferencesStore.GroqModel.entries.map { it.label }
        modelSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modelOptions)
        modelSpinner.setSelection(PreferencesStore.GroqModel.entries.indexOf(prefs.getModel()))

        val toneOptions = PreferencesStore.Tone.entries.map { it.label }
        toneSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, toneOptions)
        toneSpinner.setSelection(PreferencesStore.Tone.entries.indexOf(prefs.getTone()))

        personalityField.setText(prefs.getCustomPersonality())

        savePrefsButton.setOnClickListener {
            val model = PreferencesStore.GroqModel.entries[modelSpinner.selectedItemPosition]
            val tone = PreferencesStore.Tone.entries[toneSpinner.selectedItemPosition]
            prefs.setModel(model)
            prefs.setTone(tone)
            prefs.setCustomPersonality(personalityField.text.toString().trim())
            Toast.makeText(this, "Preferenze salvate", Toast.LENGTH_SHORT).show()
        }

        clearMemoryButton.setOnClickListener {
            lifecycleScope.launch {
                MaryenApp.instance.memoryStore.clearAll()
                Toast.makeText(this@SettingsActivity, "Memoria svuotata", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
