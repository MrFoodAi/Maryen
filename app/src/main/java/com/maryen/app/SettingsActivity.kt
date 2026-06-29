package com.maryen.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/** Schermata Impostazioni v1: inserimento API key, salvata cifrata nel Vault. */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val apiKeyField = findViewById<EditText>(R.id.apiKeyField)
        val saveButton = findViewById<Button>(R.id.saveApiKeyButton)

        val existing = MaryenApp.instance.vault.getString("anthropic_api_key")
        if (!existing.isNullOrBlank()) {
            apiKeyField.setText(existing)
        }

        saveButton.setOnClickListener {
            val key = apiKeyField.text.toString().trim()
            if (key.isNotEmpty()) {
                MaryenApp.instance.vault.putString("anthropic_api_key", key)
                Toast.makeText(this, "API key salvata", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Inserisci una API key valida", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
