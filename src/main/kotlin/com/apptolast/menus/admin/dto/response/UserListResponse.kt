package com.apptolast.menus.admin.dto.response

import com.apptolast.menus.consumer.model.enum.UserRole
import java.time.OffsetDateTime
import java.util.UUID

data class UserListResponse(
    val id: UUID,
    val emailHash: String,
    val role: UserRole,
    val isActive: Boolean,
    val createdAt: OffsetDateTime,
    val hasRestaurant: Boolean
)
