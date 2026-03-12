package com.apptolast.menus.digitalcard.repository

import com.apptolast.menus.digitalcard.model.entity.DigitalCard
import java.util.UUID

interface DigitalCardRepository {
    fun findById(id: UUID): DigitalCard?
    fun findBySlug(slug: String): DigitalCard?
    fun findAllByTenantId(tenantId: UUID): List<DigitalCard>
    fun findByMenuId(menuId: UUID): List<DigitalCard>
    fun save(entity: DigitalCard): DigitalCard
    fun deleteById(id: UUID)
    fun existsBySlug(slug: String): Boolean
}
