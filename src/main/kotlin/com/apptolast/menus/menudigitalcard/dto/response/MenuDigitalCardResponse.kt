package com.apptolast.menus.menudigitalcard.dto.response

import java.time.OffsetDateTime
import java.util.UUID

data class MenuDigitalCardResponse(
    val id: UUID,
    val menuId: UUID,
    val dishId: UUID,
    val dishName: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)
