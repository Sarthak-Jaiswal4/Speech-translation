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
        url = "https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip",
        fileName = "vosk-model-small-hi-0.22.zip",
        license = "Apache-2.0"
    )

    val teluguStt = ModelAsset(
        id = "vosk-te",
        url = "https://alphacephei.com/vosk/models/vosk-model-small-te-0.42.zip",
        fileName = "vosk-model-small-te-0.42.zip",
        license = "Apache-2.0"
    )

    val hindiToEnglishDictionary = ModelAsset(
        id = "dict-hi-en",
        url = "https://raw.githubusercontent.com/indicnlp/indicnlp_resources/master/transliteration/hi_en.json",
        fileName = "hi_en.json",
        license = "MIT"
    )

    val teluguToEnglishDictionary = ModelAsset(
        id = "dict-te-en",
        url = "https://raw.githubusercontent.com/indicnlp/indicnlp_resources/master/transliteration/te_en.json",
        fileName = "te_en.json",
        license = "MIT"
    )

    fun requiredAssets(language: SourceLanguage): List<ModelAsset> = when (language) {
        SourceLanguage.HINDI -> listOf(hindiStt, hindiToEnglishDictionary)
        SourceLanguage.TELUGU -> listOf(teluguStt, teluguToEnglishDictionary)
    }
}
