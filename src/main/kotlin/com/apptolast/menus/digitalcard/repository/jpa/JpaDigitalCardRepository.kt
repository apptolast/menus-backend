package com.apptolast.menus.digitalcard.repository.jpa

import com.apptolast.menus.digitalcard.model.entity.DigitalCard
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JpaDigitalCardRepository : JpaRepository<DigitalCard, UUID> {
    fun findBySlug(slug: String): DigitalCard?
    fun findAllByTenantId(tenantId: UUID): List<DigitalCard>
    fun findByMenuId(menuId: UUID): List<DigitalCard>
    fun existsBySlug(slug: String): Boolean
}
