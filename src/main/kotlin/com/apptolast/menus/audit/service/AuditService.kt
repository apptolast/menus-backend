package com.apptolast.menus.audit.service

import com.apptolast.menus.audit.model.entity.AllergenAuditLog
import java.util.UUID

interface AuditService {
    fun findByDish(dishId: UUID): List<AllergenAuditLog>
}
