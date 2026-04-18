package com.assignment.ondevicetranslation.data.stt

import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File

class VoskSpeechRecognizer {
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var service: SpeechService? = null

    fun initModel(modelDir: File) {
        if (!modelDir.isDirectory) {
            throw IllegalStateException("Model path is not a directory: ${modelDir.absolutePath}")
        }
        val am = File(modelDir, "am")
        val conf = File(modelDir, "conf")
        if (!am.exists() || !conf.exists()) {
            throw IllegalStateException("Invalid Vosk model folder (missing am/conf): ${modelDir.absolutePath}")
        }
        try {
            model = Model(modelDir.absolutePath)
            recognizer = Recognizer(model, 16_000.0f)
        } catch (e: Throwable) {
            throw IllegalStateException(
                "Failed to create Vosk model at ${modelDir.absolutePath}: ${e.message}",
                e
            )
        }
    }

    fun startListening(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentRecognizer = recognizer ?: return
        service = SpeechService(currentRecognizer, 16_000.0f).apply {
            startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {
                    onPartial(parseHypothesis(hypothesis))
                }

                override fun onResult(hypothesis: String?) {
                    onFinal(parseHypothesis(hypothesis))
                }

                override fun onFinalResult(hypothesis: String?) {
                    onFinal(parseHypothesis(hypothesis))
                }

                override fun onError(e: Exception?) {
                    Log.e("VoskSpeechRecognizer", "Recognition error", e)
                    onError(e?.message ?: "Unknown recognition error")
                }

                override fun onTimeout() = Unit
            })
        }
    }

    fun stopListening() {
        service?.stop()
        service = null
    }

    fun release() {
        stopListening()
        recognizer?.close()
        recognizer = null
        model?.close()
        model = null
    }

    private fun parseHypothesis(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return runCatching {
            val json = JSONObject(raw)
            json.optString("text").ifBlank { json.optString("partial", "") }
        }.getOrDefault("")
    }
}
