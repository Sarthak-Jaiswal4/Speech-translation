package com.assignment.ondevicetranslation.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class AndroidTtsEngine(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var initialized = false
    private var pendingText: String? = null
    private var pendingLocale: Locale = Locale.ENGLISH

    override fun onInit(status: Int) {
        initialized = status == TextToSpeech.SUCCESS
        if (initialized) {
            tts.setPitch(0.85f)
            tts.setSpeechRate(0.9f)
            pendingText?.let { text ->
                val loc = pendingLocale
                pendingText = null
                speak(text, loc)
            }
        }
    }

    fun speak(text: String, locale: Locale = Locale.ENGLISH) {
        if (text.isBlank()) return
        if (!initialized) {
            pendingText = text
            pendingLocale = locale
            return
        }
        applyLocale(locale)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId())
    }

    private fun applyLocale(locale: Locale) {
        tts.language = locale
    }

    private fun utteranceId(): String = "tts-${System.nanoTime()}"

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
