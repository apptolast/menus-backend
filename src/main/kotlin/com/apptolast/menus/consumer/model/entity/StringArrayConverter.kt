package com.apptolast.menus.consumer.model.entity

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class StringArrayConverter : AttributeConverter<List<String>, String> {
    override fun convertToDatabaseColumn(attribute: List<String>?): String {
        if (attribute.isNullOrEmpty()) return "{}"
        return buildString {
            append('{')
            attribute.forEachIndexed { index, element ->
                if (index > 0) {
                    append(',')
                }
                append('"')
                append(escapePostgresArrayElement(element))
                append('"')
            }
            append('}')
        }
    }

    override fun convertToEntityAttribute(dbData: String?): List<String> {
        if (dbData.isNullOrBlank() || dbData == "{}") return emptyList()
        return parsePostgresArray(dbData)
    }

    private fun escapePostgresArrayElement(element: String): String {
        val sb = StringBuilder()
        for (ch in element) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun parsePostgresArray(dbData: String): List<String> {
        val result = mutableListOf<String>()
        val s = dbData.trim()
        if (s == "{}") {
            return result
        }

        var i = 0
        val len = s.length

        // Skip leading '{' if present
        if (i < len && s[i] == '{') {
            i++
        }

        var current = StringBuilder()
        var inQuotes = false
        var escaped = false

        while (i < len) {
            val c = s[i++]

            if (escaped) {
                // Character after backslash is taken literally
                current.append(c)
                escaped = false
                continue
            }

            when (c) {
                '\\' -> {
                    escaped = true
                }
                '"' -> {
                    inQuotes = !inQuotes
                }
                ',' -> {
                    if (inQuotes) {
                        current.append(c)
                    } else {
                        result.add(current.toString())
                        current = StringBuilder()
                    }
                }
                '}' -> {
                    if (inQuotes) {
                        current.append(c)
                    } else {
                        result.add(current.toString())
                        break
                    }
                }
                else -> {
                    current.append(c)
                }
            }
        }

        return result.filter { it.isNotEmpty() }
    }
}
