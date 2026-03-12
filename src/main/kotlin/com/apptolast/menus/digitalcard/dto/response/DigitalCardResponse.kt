package com.apptolast.menus.digitalcard.dto.response

import java.time.OffsetDateTime
import java.util.UUID

data class DigitalCardResponse(
    val id: UUID,
    val menuId: UUID,
    val slug: String,
    val publicUrl: String,
    val qrCodeUrl: String?,
    val isActive: Boolean,
    val customCss: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)
