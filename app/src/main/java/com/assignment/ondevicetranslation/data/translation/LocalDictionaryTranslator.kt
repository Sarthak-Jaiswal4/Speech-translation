package com.assignment.ondevicetranslation.data.translation

import com.assignment.ondevicetranslation.data.model.SourceLanguage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File

class LocalDictionaryTranslator {
    private val dictionary: MutableMap<String, String> = mutableMapOf()

    fun load(dictionaryFile: File) {
        if (!dictionaryFile.exists()) return
        val content = dictionaryFile.readText()
        val jsonObj = Json.parseToJsonElement(content).jsonObject
        dictionary.clear()
        jsonObj.forEach { (k, v) ->
            dictionary[k.lowercase()] = v.toString().replace("\"", "")
        }
    }

    fun loadBuiltIn(language: SourceLanguage) {
        dictionary.clear()
        when (language) {
            SourceLanguage.HINDI -> dictionary.putAll(hindiDictionary)
            SourceLanguage.TELUGU -> dictionary.putAll(teluguDictionary)
        }
    }

    fun translateToEnglish(source: String): String {
        if (source.isBlank()) return ""
        return source
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                dictionary[token.lowercase()] ?: token
            }
    }

    companion object {
        private val hindiDictionary = mapOf(
            "नमस्ते" to "hello",
            "namaste" to "hello",
            "धन्यवाद" to "thank you",
            "dhanyavaad" to "thank you",
            "हाँ" to "yes",
            "haan" to "yes",
            "नहीं" to "no",
            "nahi" to "no",
            "मैं" to "I",
            "तुम" to "you",
            "वह" to "he/she",
            "हम" to "we",
            "यह" to "this",
            "वो" to "that",
            "क्या" to "what",
            "कहाँ" to "where",
            "कब" to "when",
            "कैसे" to "how",
            "क्यों" to "why",
            "पानी" to "water",
            "खाना" to "food",
            "घर" to "home",
            "काम" to "work",
            "समय" to "time",
            "दिन" to "day",
            "रात" to "night",
            "सुबह" to "morning",
            "शाम" to "evening",
            "अच्छा" to "good",
            "बुरा" to "bad",
            "बड़ा" to "big",
            "छोटा" to "small",
            "आज" to "today",
            "aaj" to "today",
            "कल" to "tomorrow"
        )

        private val teluguDictionary = mapOf(
            "నమస్కారం" to "hello",
            "ధన్యవాదాలు" to "thank you",
            "అవును" to "yes",
            "కాదు" to "no",
            "నేను" to "I",
            "నువ్వు" to "you",
            "అతను" to "he",
            "ఆమె" to "she",
            "మేము" to "we",
            "ఇది" to "this",
            "అది" to "that",
            "ఏమి" to "what",
            "ఎక్కడ" to "where",
            "ఎప్పుడు" to "when",
            "ఎలా" to "how",
            "ఎందుకు" to "why",
            "నీరు" to "water",
            "భోజనం" to "food",
            "ఇల్లు" to "home",
            "పని" to "work",
            "సమయం" to "time",
            "రోజు" to "day",
            "రాత్రి" to "night",
            "ఉదయం" to "morning",
            "సాయంత్రం" to "evening",
            "మంచిది" to "good",
            "చెడ్డది" to "bad",
            "పెద్ద" to "big",
            "చిన్న" to "small",
            "ఈరోజు" to "today",
            "రేపు" to "tomorrow"
        )
    }
}