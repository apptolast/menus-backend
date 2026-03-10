package com.apptolast.menus.consumer.model.entity

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class StringArrayConverter : AttributeConverter<List<String>, String> {
    override fun convertToDatabaseColumn(attribute: List<String>?): String {
        if (attribute.isNullOrEmpty()) return "{}"
        return "{${attribute.joinToString(",") { "\"$it\"" }}}"
    }

    override fun convertToEntityAttribute(dbData: String?): List<String> {
        if (dbData.isNullOrBlank() || dbData == "{}") return emptyList()
        return dbData.trim('{', '}').split(",").map { it.trim('"') }.filter { it.isNotEmpty() }
    }
}
