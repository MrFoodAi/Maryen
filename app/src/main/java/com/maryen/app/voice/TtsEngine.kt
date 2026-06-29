package com.maryen.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * TTS v1: motore TextToSpeech nativo di Android.
 * Niente Sherpa-ONNX/VITS, niente modello da scaricare: usa la voce
 * italiana già installata nel sistema (Google: spesso femminile di default,
 * ma dipende dal telefono — selezionabile in Impostazioni > Lingue > Sintesi vocale).
 */
class TtsEngine(ctx: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ITALIAN
                ready = true
            }
        }
    }

    suspend fun speak(text: String) {
        if (!ready || text.isBlank()) return
        suspendCancellableCoroutine<Unit> { cont ->
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (cont.isActive) cont.resume(Unit)
                }
                @Deprecated("deprecated in API")
                override fun onError(utteranceId: String?) {
                    if (cont.isActive) cont.resume(Unit)
                }
            })
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "maryen_utt_${System.currentTimeMillis()}")
        }
    }

    fun stop() { tts?.stop() }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
