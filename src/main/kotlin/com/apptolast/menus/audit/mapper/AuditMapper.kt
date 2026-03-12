package com.apptolast.menus.audit.mapper

import com.apptolast.menus.audit.dto.response.AuditLogResponse
import com.apptolast.menus.audit.model.entity.AllergenAuditLog

fun AllergenAuditLog.toResponse() = AuditLogResponse(
    id = id,
    dishId = dishId,
    allergenId = allergenId,
    changedByUuid = changedByUuid,
    action = action,
    oldLevel = oldLevel,
    newLevel = newLevel,
    changedAt = changedAt
)
