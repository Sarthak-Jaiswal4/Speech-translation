package com.assignment.ondevicetranslation.data.translation

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

    fun translateToEnglish(source: String): String {
        if (source.isBlank()) return ""
        return source
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                dictionary[token.lowercase()] ?: token
            }
    }
}
