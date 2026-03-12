package com.apptolast.menus.digitalcard.repository

import com.apptolast.menus.digitalcard.model.entity.DigitalCard
import com.apptolast.menus.digitalcard.repository.jpa.JpaDigitalCardRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class DigitalCardRepositoryAdapter(
    private val jpa: JpaDigitalCardRepository
) : DigitalCardRepository {
    override fun findById(id: UUID): DigitalCard? = jpa.findById(id).orElse(null)
    override fun findBySlug(slug: String): DigitalCard? = jpa.findBySlug(slug)
    override fun findAllByTenantId(tenantId: UUID): List<DigitalCard> = jpa.findAllByTenantId(tenantId)
    override fun findByMenuId(menuId: UUID): List<DigitalCard> = jpa.findByMenuId(menuId)
    override fun save(entity: DigitalCard): DigitalCard = jpa.save(entity)
    override fun deleteById(id: UUID) = jpa.deleteById(id)
    override fun existsBySlug(slug: String): Boolean = jpa.existsBySlug(slug)
}
