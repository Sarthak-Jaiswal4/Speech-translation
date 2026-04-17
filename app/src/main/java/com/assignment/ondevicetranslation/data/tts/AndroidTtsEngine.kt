package com.assignment.ondevicetranslation.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class AndroidTtsEngine(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var initialized = false

    override fun onInit(status: Int) {
        initialized = status == TextToSpeech.SUCCESS
        if (initialized) {
            tts.language = Locale.ENGLISH
        }
    }

    fun speak(text: String) {
        if (!initialized || text.isBlank()) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "translated-output")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
