package com.apptolast.menus.digitalcard.service

import com.apptolast.menus.digitalcard.model.entity.DigitalCard
import java.util.UUID

interface DigitalCardService {
    fun findAll(): List<DigitalCard>
    fun findById(id: UUID): DigitalCard
    fun findBySlug(slug: String): DigitalCard
    fun create(card: DigitalCard): DigitalCard
    fun update(id: UUID, card: DigitalCard): DigitalCard
    fun delete(id: UUID)
}
