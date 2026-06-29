package com.maryen.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.maryen.app.voice.SpeechInput
import kotlinx.coroutines.launch

/**
 * Schermata principale v1: lista messaggi + campo testo + pulsante microfono.
 * Niente Compose in v1 per restare sul minimo indispensabile e ridurre
 * le dipendenze Gradle da configurare nella prima build; il layout
 * usa Views classiche (activity_main.xml).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var chatAdapter: ArrayAdapter<String>
    private lateinit var listView: ListView
    private lateinit var input: EditText
    private lateinit var micButton: ImageButton
    private lateinit var sendButton: Button

    private val speechInput by lazy { SpeechInput(this) }

    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startListening() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.chatList)
        input = findViewById(R.id.inputText)
        micButton = findViewById(R.id.micButton)
        sendButton = findViewById(R.id.sendButton)

        chatAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = chatAdapter

        sendButton.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                input.setText("")
                handleUserText(text)
            }
        }

        micButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                startListening()
            } else {
                micPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        checkApiKey()
    }

    private fun checkApiKey() {
        val key = MaryenApp.instance.vault.getString("anthropic_api_key")
        if (key.isNullOrBlank()) {
            addMessage("Maryen", "Non ho ancora una API key configurata. Vai in Impostazioni per inserirla, altrimenti non posso rispondere.")
        }
    }

    private fun startListening() {
        addMessage("Maryen", "Ti ascolto…")
        lifecycleScope.launch {
            val text = speechInput.listenOnce()
            if (text.isNotBlank()) {
                handleUserText(text)
            }
        }
    }

    private fun handleUserText(text: String) {
        addMessage("Tu", text)
        lifecycleScope.launch {
            MaryenApp.instance.orchestrator.handle(text).collect { reply ->
                addMessage("Maryen", reply)
            }
        }
    }

    private fun addMessage(sender: String, text: String) {
        chatAdapter.add("$sender: $text")
        listView.smoothScrollToPosition(chatAdapter.count - 1)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add("Impostazioni")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        startActivity(Intent(this, SettingsActivity::class.java))
        return true
    }
}
