package com.apptolast.menus.consumer.model.entity

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class StringArrayConverter : AttributeConverter<List<String>, String> {
    override fun convertToDatabaseColumn(attribute: List<String>?): String {
        if (attribute.isNullOrEmpty()) return "{}"
        val escapedElements = attribute.map { escapeArrayElement(it) }
        return "{${escapedElements.joinToString(",")}}"
    }

    override fun convertToEntityAttribute(dbData: String?): List<String> {
        if (dbData.isNullOrBlank() || dbData == "{}") return emptyList()
        return parsePostgresArray(dbData).filter { it.isNotEmpty() }
    }

    /**
     * Escapes a single element for inclusion in a Postgres array literal.
     * Always wraps the element in double quotes and escapes backslashes and quotes.
     */
    private fun escapeArrayElement(element: String): String {
        val escaped = element
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        return "\"$escaped\""
    }

    /**
     * Parses a one-dimensional Postgres array literal into a list of strings.
     * Handles quoted elements and backslash escapes for quotes and backslashes.
     */
    private fun parsePostgresArray(dbData: String): List<String> {
        if (dbData.length < 2 || dbData.first() != '{' || dbData.last() != '}') {
            return listOf(dbData)
        }

        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var escapeNext = false

        for (i in 1 until dbData.length - 1) {
            val ch = dbData[i]

            if (escapeNext) {
                current.append(ch)
                escapeNext = false
                continue
            }

            if (ch == '\\') {
                if (inQuotes) {
                    // Inside quoted elements, backslash escapes the next character
                    escapeNext = true
                }
                // Outside quotes, unquoted elements in Postgres arrays don't use backslash escaping;
                // ignore the backslash to match Postgres parsing behavior
                continue
            }

            if (ch == '"') {
                inQuotes = !inQuotes
                continue
            }

            if (ch == ',' && !inQuotes) {
                result.add(current.toString())
                current.setLength(0)
                continue
            }

            current.append(ch)
        }

        if (current.isNotEmpty()) {
            result.add(current.toString())
        }

        return result
    }
}
