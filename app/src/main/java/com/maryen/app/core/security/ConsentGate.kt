package com.maryen.app.core.security

import android.content.Context

/**
 * Cancello di consenso semplificato per la v1.
 * Le skill marcate "requiresConsent = true" vengono bloccate finché
 * Enrico non le abilita esplicitamente dal pannello Impostazioni.
 * Versione v1: consenso persistente per-skill (non per singola chiamata),
 * salvato in chiaro nelle SharedPreferences — nessun dato sensibile qui dentro,
 * solo flag booleani "skill X abilitata".
 */
class ConsentGate(ctx: Context) {

    private val prefs = ctx.getSharedPreferences("maryen_consent", Context.MODE_PRIVATE)

    fun granted(skillId: String): Boolean =
        prefs.getBoolean("consent_$skillId", false)

    fun grant(skillId: String) {
        prefs.edit().putBoolean("consent_$skillId", true).apply()
    }

    fun revoke(skillId: String) {
        prefs.edit().putBoolean("consent_$skillId", false).apply()
    }

    fun allGranted(): Map<String, Boolean> =
        prefs.all.filterKeys { it.startsWith("consent_") }
            .mapKeys { it.key.removePrefix("consent_") }
            .mapValues { it.value as? Boolean ?: false }
}
