package com.apptolast.menus.audit.dto.response

import com.apptolast.menus.audit.model.entity.AuditAction
import com.apptolast.menus.dish.model.enum.ContainmentLevel
import java.time.OffsetDateTime
import java.util.UUID

data class AuditLogResponse(
    val id: UUID,
    val dishId: UUID,
    val allergenId: Int,
    val changedByUuid: UUID,
    val action: AuditAction,
    val oldLevel: ContainmentLevel?,
    val newLevel: ContainmentLevel?,
    val changedAt: OffsetDateTime
)
