package com.assignment.ondevicetranslation.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.assignment.ondevicetranslation.data.download.ModelDownloadManager
import com.assignment.ondevicetranslation.data.model.ModelCatalog
import com.assignment.ondevicetranslation.data.model.SourceLanguage
import com.assignment.ondevicetranslation.data.stt.VoskSpeechRecognizer
import com.assignment.ondevicetranslation.data.translation.LocalDictionaryTranslator
import com.assignment.ondevicetranslation.data.tts.AndroidTtsEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val downloader = ModelDownloadManager(application)
    private val recognizer = VoskSpeechRecognizer()
    private val translator = LocalDictionaryTranslator()
    private val tts = AndroidTtsEngine(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun setLanguage(language: SourceLanguage) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun initializeSelectedLanguage() {
        val language = _uiState.value.selectedLanguage
        viewModelScope.launch {
            _uiState.update { it.copy(status = "Downloading models...") }
            try {
                val rootDir = downloader.ensureAssets(ModelCatalog.requiredAssets(language)) { progress, status ->
                    _uiState.update {
                        it.copy(downloadProgress = progress, downloadStatus = status)
                    }
                }

                val modelDir = resolveSttDir(rootDir, language)
                recognizer.initModel(modelDir)

                val dictionaryFile = resolveDictionary(rootDir, language)
                translator.load(dictionaryFile)

                _uiState.update {
                    it.copy(status = "Ready", downloadStatus = "Models ready")
                }
            } catch (error: Exception) {
                _uiState.update { it.copy(status = "Error: ${error.message}") }
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
                _uiState.update { it.copy(sourceText = partial, status = "Listening...") }
            },
            onFinal = { transcript ->
                val translated = translator.translateToEnglish(transcript)
                tts.speak(translated)
                _uiState.update {
                    it.copy(
                        sourceText = transcript,
                        translatedText = translated,
                        isListening = true,
                        status = "Translated"
                    )
                }
            }
        )
        _uiState.update { it.copy(isListening = true, status = "Listening...") }
    }

    private fun resolveSttDir(rootDir: File, language: SourceLanguage): File {
        val zipName = when (language) {
            SourceLanguage.HINDI -> ModelCatalog.hindiStt.fileName
            SourceLanguage.TELUGU -> ModelCatalog.teluguStt.fileName
        }
        return File(rootDir, zipName.removeSuffix(".zip"))
            .walkTopDown()
            .firstOrNull { File(it, "am").exists() || File(it, "conf").exists() }
            ?: File(rootDir, zipName.removeSuffix(".zip"))
    }

    private fun resolveDictionary(rootDir: File, language: SourceLanguage): File {
        val fileName = when (language) {
            SourceLanguage.HINDI -> ModelCatalog.hindiToEnglishDictionary.fileName
            SourceLanguage.TELUGU -> ModelCatalog.teluguToEnglishDictionary.fileName
        }
        return File(rootDir, fileName)
    }

    override fun onCleared() {
        recognizer.release()
        tts.shutdown()
        super.onCleared()
    }
}
