package com.assignment.ondevicetranslation.ui

import com.assignment.ondevicetranslation.data.model.SourceLanguage

data class MainUiState(
    val isListening: Boolean = false,
    val selectedLanguage: SourceLanguage = SourceLanguage.HINDI,
    val sourceText: String = "",
    val translatedText: String = "",
    val status: String = "Idle",
    val downloadProgress: Int = 0,
    val downloadStatus: String = "Models not downloaded"
)
