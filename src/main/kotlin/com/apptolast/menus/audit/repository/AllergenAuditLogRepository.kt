package com.apptolast.menus.audit.repository

import com.apptolast.menus.audit.model.entity.AllergenAuditLog
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AllergenAuditLogRepository : JpaRepository<AllergenAuditLog, UUID> {
    fun findByDishIdOrderByChangedAtDesc(dishId: UUID): List<AllergenAuditLog>
    fun findByTenantIdOrderByChangedAtDesc(tenantId: UUID): List<AllergenAuditLog>
}
