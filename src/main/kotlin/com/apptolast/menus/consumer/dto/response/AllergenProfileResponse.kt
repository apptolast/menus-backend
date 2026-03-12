package com.apptolast.menus.consumer.dto.response

import java.time.OffsetDateTime
import java.util.UUID

data class AllergenProfileResponse(
    val userId: UUID,
    val allergenCodes: List<String> = emptyList(),
    val severityNotes: String = "",
    val updatedAt: OffsetDateTime? = null
)
