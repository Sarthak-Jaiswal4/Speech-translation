package com.assignment.ondevicetranslation.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.assignment.ondevicetranslation.data.download.ModelDownloadManager
import com.assignment.ondevicetranslation.data.model.ModelCatalog
import com.assignment.ondevicetranslation.data.model.SourceLanguage
import com.assignment.ondevicetranslation.data.stt.VoskSpeechRecognizer
import com.assignment.ondevicetranslation.data.translation.LocalDictionaryTranslator
import com.assignment.ondevicetranslation.data.translation.MlKitOfflineTranslator
import com.assignment.ondevicetranslation.data.tts.AndroidTtsEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val downloader = ModelDownloadManager(application)
    private val recognizer = VoskSpeechRecognizer()
    private val translator = LocalDictionaryTranslator()
    private val mlKitTranslator = MlKitOfflineTranslator()
    private val tts = AndroidTtsEngine(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Problem 3: TTS speaking callbacks
        tts.setOnSpeakingStartListener {
            _uiState.update { it.copy(isSpeaking = true) }
        }
        tts.setOnSpeakingDoneListener {
            _uiState.update { it.copy(isSpeaking = false) }
        }
        // Problem 2: Check offline readiness on start
        _uiState.update { it.copy(offlineStatus = checkOfflineStatus()) }
    }

    fun setLanguage(language: SourceLanguage) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun initializeSelectedLanguage() {
        val language = _uiState.value.selectedLanguage
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    status = "Downloading models...",
                    downloadProgress = 0f,
                    downloadStatus = "Preparing ${language.displayName} model... 0%",
                    offlineStatus = OfflineStatus.DOWNLOADING
                )
            }
            try {
                val rootDir = downloader.ensureAssets(ModelCatalog.requiredAssets(language)) { progress, status ->
                    _uiState.update {
                        it.copy(downloadProgress = progress / 100f, downloadStatus = status)
                    }
                }

                val modelDir = resolveSttDir(rootDir, language)
                recognizer.initModel(modelDir)

                // Load built-in dictionary instead of downloaded file
                translator.loadBuiltIn(language)
                mlKitTranslator.prepare(language)

                _uiState.update {
                    it.copy(
                        status = "Ready",
                        downloadStatus = "Models ready",
                        downloadProgress = 1f,
                        offlineStatus = checkOfflineStatus()
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        status = "Error: ${error.message}",
                        offlineStatus = checkOfflineStatus()
                    )
                }
            }
        }
    }

    fun toggleListening() {
        if (_uiState.value.isListening) {
            recognizer.stopListening()
            _uiState.update { it.copy(isListening = false, status = "Stopped") }
            return
        }
        recognizer.startListening(
            onPartial = { partial ->
                if (partial.isNotBlank()) {
                    _uiState.update { it.copy(sourceText = partial, status = "Listening...") }
                }
            },
            onFinal = { transcript ->
                recognizer.stopListening()
                _uiState.update { it.copy(isListening = false) }
                if (transcript.isBlank()) {
                    _uiState.update {
                        it.copy(status = "Didn't catch speech. Please speak clearly and retry.")
                    }
                    return@startListening
                }
                _uiState.update { it.copy(sourceText = transcript, status = "Translating...") }
                viewModelScope.launch {
                    val translated = mlKitTranslator.translate(transcript)
                        ?: translator.translateToEnglish(transcript)
                    val spokenText = if (translated.isBlank()) {
                        "Text received successfully."
                    } else {
                        "Text received successfully. $translated"
                    }
                    tts.speak(spokenText, Locale.ENGLISH)
                    _uiState.update {
                        it.copy(
                            sourceText = transcript,
                            translatedText = translated,
                            status = "Text received and translated"
                        )
                    }
                }
            },
            onError = { message ->
                _uiState.update {
                    it.copy(isListening = false, status = "Speech error: $message")
                }
            }
        )
        _uiState.update { it.copy(isListening = true, status = "Listening...") }
    }

    fun speakTypedTextAsEnglishTranslation(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(status = "Type some text first.") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(sourceText = trimmed, status = "Translating typed text...")
            }
            val translated = mlKitTranslator.translate(trimmed)
                ?: translator.translateToEnglish(trimmed)
            val toSpeak = translated.ifBlank { trimmed }
            tts.speak(toSpeak, Locale.ENGLISH)
            _uiState.update {
                it.copy(
                    translatedText = translated,
                    status = if (translated.isBlank()) "Speaking typed text" else "Speaking English translation"
                )
            }
        }
    }

    fun speakTypedTextInSourceLanguage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(status = "Type some text first.") }
            return
        }
        val locale = when (_uiState.value.selectedLanguage) {
            SourceLanguage.HINDI -> Locale("hi", "IN")
            SourceLanguage.TELUGU -> Locale("te", "IN")
        }
        tts.speak(trimmed, locale)
        _uiState.update { state ->
            state.copy(
                sourceText = trimmed,
                status = "Speaking in ${state.selectedLanguage.displayName}"
            )
        }
    }

    // ── Problem 2: Offline readiness check ─────────────────────
    private fun checkOfflineStatus(): OfflineStatus {
        val modelsDir = File(getApplication<Application>().filesDir, "models")
        if (!modelsDir.exists()) return OfflineStatus.NOT_DOWNLOADED

        val hindiBase = File(modelsDir, ModelCatalog.hindiStt.fileName.removeSuffix(".zip"))
        val teluguBase = File(modelsDir, ModelCatalog.teluguStt.fileName.removeSuffix(".zip"))

        val hindiReady = ModelDownloadManager.findVoskModelRoot(hindiBase) != null
        val teluguReady = ModelDownloadManager.findVoskModelRoot(teluguBase) != null

        return if (hindiReady && teluguReady) OfflineStatus.READY
        else OfflineStatus.NOT_DOWNLOADED
    }

    private fun resolveSttDir(rootDir: File, language: SourceLanguage): File {
        val zipName = when (language) {
            SourceLanguage.HINDI -> ModelCatalog.hindiStt.fileName
            SourceLanguage.TELUGU -> ModelCatalog.teluguStt.fileName
        }
        val extractedBase = File(rootDir, zipName.removeSuffix(".zip"))
        return ModelDownloadManager.findVoskModelRoot(extractedBase)
            ?: throw IllegalStateException("Vosk model not found under ${extractedBase.absolutePath}")
    }

    override fun onCleared() {
        recognizer.release()
        mlKitTranslator.shutdown()
        tts.shutdown()
        super.onCleared()
    }
}