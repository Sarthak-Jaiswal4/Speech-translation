package com.assignment.ondevicetranslation.ui

import com.assignment.ondevicetranslation.data.model.SourceLanguage

data class MainUiState(
    val isListening: Boolean = false,
    val selectedLanguage: SourceLanguage = SourceLanguage.HINDI,
    val sourceText: String = "",
    val translatedText: String = "",
    val status: String = "Idle",
    val downloadProgress: Float = 0f,
    val downloadStatus: String = "Models not downloaded"
)
