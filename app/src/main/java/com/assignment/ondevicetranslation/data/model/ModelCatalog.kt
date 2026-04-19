package com.assignment.ondevicetranslation.data.model

enum class SourceLanguage(val displayName: String) {
    HINDI("Hindi"),
    TELUGU("Telugu")
}

data class ModelAsset(
    val id: String,
    val url: String,
    val fileName: String,
    val license: String
)

object ModelCatalog {
    val hindiStt = ModelAsset(
        id = "vosk-hi",
        url = "https://github.com/Sarthak-Jaiswal4/Speech-translation/releases/download/v1.0/vosk-model-small-hi-0.22.zip",
        fileName = "vosk-model-small-hi-0.22.zip",
        license = "Apache-2.0"
    )

    val teluguStt = ModelAsset(
        id = "vosk-te",
        url = "https://github.com/Sarthak-Jaiswal4/Speech-translation/releases/download/v1.0/vosk-model-small-te-0.42.zip",
        fileName = "vosk-model-small-te-0.42.zip",
        license = "Apache-2.0"
    )

    fun requiredAssets(language: SourceLanguage): List<ModelAsset> = when (language) {
        SourceLanguage.HINDI -> listOf(hindiStt)
        SourceLanguage.TELUGU -> listOf(teluguStt)
    }
}