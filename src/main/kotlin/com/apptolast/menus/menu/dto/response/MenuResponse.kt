package com.apptolast.menus.menu.dto.response

import java.time.OffsetDateTime
import java.util.UUID

data class MenuResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val isArchived: Boolean,
    val displayOrder: Int,
    val sections: List<SectionResponse> = emptyList(),
    val updatedAt: OffsetDateTime
)
