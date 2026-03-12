package com.apptolast.menus.audit.service.impl

import com.apptolast.menus.audit.model.entity.AllergenAuditLog
import com.apptolast.menus.audit.repository.AllergenAuditLogRepository
import com.apptolast.menus.audit.service.AuditService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AuditServiceImpl(
    private val allergenAuditLogRepository: AllergenAuditLogRepository
) : AuditService {
    override fun findByDish(dishId: UUID): List<AllergenAuditLog> =
        allergenAuditLogRepository.findByDishIdOrderByChangedAtDesc(dishId)

    override fun findByTenant(tenantId: UUID, pageable: Pageable): Page<AllergenAuditLog> =
        allergenAuditLogRepository.findByTenantId(tenantId, pageable)
}
