package com.example.learnthai.audio

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

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
