package com.assignment.ondevicetranslation.data.translation

import com.assignment.ondevicetranslation.data.model.SourceLanguage
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitOfflineTranslator {
    private var translator: Translator? = null
    private var activeLanguage: SourceLanguage? = null

    suspend fun prepare(language: SourceLanguage) = withContext(Dispatchers.IO) {
        if (activeLanguage == language && translator != null) return@withContext

        translator?.close()
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(language.toMlKitLanguage())
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()

        val newTranslator = Translation.getClient(options)
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        newTranslator.downloadModelIfNeeded(conditions).await()
        translator = newTranslator
        activeLanguage = language
    }

    suspend fun translate(source: String): String? = withContext(Dispatchers.IO) {
        val activeTranslator = translator ?: return@withContext null
        if (source.isBlank()) return@withContext ""
        runCatching { activeTranslator.translate(source).await().trim() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    fun shutdown() {
        translator?.close()
        translator = null
        activeLanguage = null
    }

    private fun SourceLanguage.toMlKitLanguage(): String = when (this) {
        SourceLanguage.HINDI -> TranslateLanguage.HINDI
        SourceLanguage.TELUGU -> TranslateLanguage.TELUGU
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result -> continuation.resume(result) }
    addOnFailureListener { error -> continuation.resumeWithException(error) }
    addOnCanceledListener { continuation.cancel() }
}
