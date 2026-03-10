package com.apptolast.menus.restaurant.dto.response

import java.time.OffsetDateTime
import java.util.UUID

data class RestaurantResponse(
    val id: UUID,
    val name: String,
    val slug: String,
    val description: String?,
    val address: String?,
    val phone: String?,
    val logoUrl: String?,
    val isActive: Boolean,
    val createdAt: OffsetDateTime
)
