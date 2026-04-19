package com.assignment.ondevicetranslation.data.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class AndroidTtsEngine(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var initialized = false
    private var pendingText: String? = null
    private var pendingLocale: Locale = Locale.ENGLISH

    /** Called on main thread when an utterance finishes (done or error). */
    private var onSpeakingDone: (() -> Unit)? = null

    /** Called on main thread when an utterance starts. */
    private var onSpeakingStart: (() -> Unit)? = null

    fun setOnSpeakingDoneListener(listener: (() -> Unit)?) {
        onSpeakingDone = listener
    }

    fun setOnSpeakingStartListener(listener: (() -> Unit)?) {
        onSpeakingStart = listener
    }

    override fun onInit(status: Int) {
        initialized = status == TextToSpeech.SUCCESS
        if (initialized) {
            tts.setPitch(0.85f)
            tts.setSpeechRate(0.9f)

            // Register utterance progress listener for speaking feedback
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeakingStart?.invoke()
                }
                override fun onDone(utteranceId: String?) {
                    onSpeakingDone?.invoke()
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onSpeakingDone?.invoke()
                }
            })

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
        val params = Bundle()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId())
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
