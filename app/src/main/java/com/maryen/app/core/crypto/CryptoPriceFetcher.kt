package com.maryen.app.core.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CoinPrice(val id: String, val label: String, val usd: Double, val change24h: Double)

class CryptoPriceFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val coins = listOf(
        "bitcoin" to "BTC",
        "ethereum" to "ETH",
        "binancecoin" to "BNB",
        "matic-network" to "MATIC",
        "solana" to "SOL"
    )

    suspend fun fetchPrices(): List<CoinPrice> = withContext(Dispatchers.IO) {
        val ids = coins.joinToString(",") { it.first }
        val url = "https://api.coingecko.com/api/v3/simple/price" +
            "?ids=$ids&vs_currencies=usd&include_24hr_change=true"

        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("Errore CoinGecko (${resp.code})")
            }
            val json = JSONObject(resp.body?.string().orEmpty())
            coins.mapNotNull { (id, label) ->
                val coinJson = json.optJSONObject(id) ?: return@mapNotNull null
                CoinPrice(
                    id = id,
                    label = label,
                    usd = coinJson.optDouble("usd", 0.0),
                    change24h = coinJson.optDouble("usd_24h_change", 0.0)
                )
            }
        }
    }
}
