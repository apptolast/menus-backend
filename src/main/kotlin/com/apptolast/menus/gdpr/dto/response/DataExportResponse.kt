package com.apptolast.menus.gdpr.dto.response

import java.time.OffsetDateTime
import java.util.UUID

data class DataExportResponse(
    val userId: UUID,
    val email: String,
    val role: String,
    val allergenProfile: AllergenProfileExport?,
    val exportedAt: OffsetDateTime = OffsetDateTime.now()
)

data class AllergenProfileExport(
    val allergenCodes: List<String>,
    val severityNotes: String?
)
