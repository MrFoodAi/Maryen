package com.maryen.app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.maryen.app.core.crypto.CryptoPriceFetcher
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CryptoActivity : AppCompatActivity() {

    private val fetcher = CryptoPriceFetcher()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crypto)

        val listView = findViewById<ListView>(R.id.cryptoList)
        val statusText = findViewById<TextView>(R.id.cryptoStatus)
        val refreshButton = findViewById<Button>(R.id.refreshButton)

        fun load() {
            statusText.text = "Aggiornamento…"
            lifecycleScope.launch {
                try {
                    val prices = fetcher.fetchPrices()
                    val fmt = NumberFormat.getCurrencyInstance(Locale.US)
                    val rows = prices.map { p ->
                        val arrow = if (p.change24h >= 0) "▲" else "▼"
                        "${p.label}: ${fmt.format(p.usd)}  $arrow ${"%.2f".format(p.change24h)}%"
                    }
                    listView.adapter = ArrayAdapter(this@CryptoActivity, android.R.layout.simple_list_item_1, rows)
                    statusText.text = "Aggiornato — dati CoinGecko (pubblico, no trading)"
                } catch (e: Exception) {
                    statusText.text = "Errore: ${e.message ?: "impossibile aggiornare"}"
                }
            }
        }

        refreshButton.setOnClickListener { load() }
        load()
    }
}
