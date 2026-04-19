package com.assignment.ondevicetranslation.ui

import com.assignment.ondevicetranslation.data.model.SourceLanguage

/** Offline readiness indicator shown as a colored badge in the UI. */
enum class OfflineStatus {
    NOT_DOWNLOADED,   // 🔴 — models not on device
    DOWNLOADING,      // 🟡 — currently downloading
    READY             // 🟢 — fully ready for offline use
}

data class MainUiState(
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val selectedLanguage: SourceLanguage = SourceLanguage.HINDI,
    val sourceText: String = "",
    val translatedText: String = "",
    val status: String = "Idle",
    val downloadProgress: Float = 0f,
    val downloadStatus: String = "Models not downloaded",
    val offlineStatus: OfflineStatus = OfflineStatus.NOT_DOWNLOADED
)
