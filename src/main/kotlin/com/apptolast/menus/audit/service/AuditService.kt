package com.apptolast.menus.audit.service

import com.apptolast.menus.audit.model.entity.AllergenAuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface AuditService {
    fun findByDish(dishId: UUID): List<AllergenAuditLog>
    fun findByTenant(tenantId: UUID, pageable: Pageable): Page<AllergenAuditLog>
}
