package com.apptolast.menus.consumer.dto.request

data class AllergenProfileRequest(
    val allergenCodes: List<String> = emptyList(),
    val severityNotes: String? = null
)
