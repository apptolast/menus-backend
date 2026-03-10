package com.apptolast.menus.consumer.dto.response

import java.time.OffsetDateTime
import java.util.UUID

data class AllergenProfileResponse(
    val profileUuid: UUID,
    val allergenCodes: List<String>,
    val severityNotes: String?,
    val updatedAt: OffsetDateTime
)
