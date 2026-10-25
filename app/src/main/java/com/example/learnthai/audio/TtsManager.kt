package com.example.learnthai.audio

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.suspendCancellableCoroutine

class TtsManager(
    private val context: Context,
    private val onInitComplete: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    var isReady: Boolean = false
        private set

    var speechRate: Float = 0.85f
        set(value) {
            field = value.coerceIn(0.5f, 1.2f)
            tts?.setSpeechRate(field)
        }

    init {
        tts = TextToSpeech(context.applicationContext, this, "com.google.android.tts")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val thaiLocale = Locale("th", "TH")
            val result = tts?.setLanguage(thaiLocale)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // If Google engine lacks data, try default device engine
                tts = TextToSpeech(context.applicationContext) { fallbackStatus ->
                    if (fallbackStatus == TextToSpeech.SUCCESS) {
                        tts?.setLanguage(thaiLocale)
                        tts?.setSpeechRate(speechRate)
                        isReady = true
                        onInitComplete(true)
                    } else {
                        isReady = false
                        onInitComplete(false)
                    }
                }
            } else {
                tts?.setSpeechRate(speechRate)
                isReady = true
                onInitComplete(true)
            }
        } else {
            isReady = false
            onInitComplete(false)
        }
    }

    fun speak(text: String, rate: Float = speechRate) {
        if (!isReady || tts == null) return
        tts?.setSpeechRate(rate)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_THAI_${System.currentTimeMillis()}")
    }

    /**
     * Synthesizes [text] to a WAV file in the cache dir and returns it when complete,
     * or null if the engine does not support file synthesis.
     */
    suspend fun synthesizeToFile(text: String, rate: Float = speechRate): File? =
        suspendCancellableCoroutine { cont ->
            val engine = tts
            if (!isReady || engine == null) {
                cont.resumeWith(Result.success<File?>(null))
                return@suspendCancellableCoroutine
            }

            engine.setSpeechRate(rate)
            val file = File(context.cacheDir, "ref_synth_${System.currentTimeMillis()}.wav")
            val utteranceId = "REF_${System.currentTimeMillis()}"
            val finished = AtomicBoolean(false)

            val listener = object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    if (!finished.compareAndSet(false, true)) return
                    try { engine.setOnUtteranceProgressListener(null) } catch (_: Exception) {}
                    if (cont.isActive) cont.resumeWith(Result.success<File?>(file))
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (!finished.compareAndSet(false, true)) return
                    try { engine.setOnUtteranceProgressListener(null) } catch (_: Exception) {}
                    if (cont.isActive) cont.resumeWith(Result.success<File?>(null))
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (!finished.compareAndSet(false, true)) return
                    try { engine.setOnUtteranceProgressListener(null) } catch (_: Exception) {}
                    if (cont.isActive) cont.resumeWith(Result.success<File?>(null))
                }
            }

            engine.setOnUtteranceProgressListener(listener)
            val result = try {
                engine.synthesizeToFile(text, null, file, utteranceId)
            } catch (e: Exception) {
                TextToSpeech.ERROR
            }

            if (result != TextToSpeech.SUCCESS && finished.compareAndSet(false, true)) {
                try { engine.setOnUtteranceProgressListener(null) } catch (_: Exception) {}
                if (cont.isActive) cont.resumeWith(Result.success<File?>(null))
            }

            cont.invokeOnCancellation {
                try { engine.setOnUtteranceProgressListener(null) } catch (_: Exception) {}
            }
        }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }

    fun getInstallTtsDataIntent(): Intent {
        return Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
    }
}
