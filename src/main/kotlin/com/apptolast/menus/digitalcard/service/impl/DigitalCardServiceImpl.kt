package com.apptolast.menus.digitalcard.service.impl

import com.apptolast.menus.config.TenantContext
import com.apptolast.menus.digitalcard.model.entity.DigitalCard
import com.apptolast.menus.digitalcard.repository.DigitalCardRepository
import com.apptolast.menus.digitalcard.service.DigitalCardService
import com.apptolast.menus.menu.repository.MenuRepository
import com.apptolast.menus.shared.exception.BusinessValidationException
import com.apptolast.menus.shared.exception.ConflictException
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class DigitalCardServiceImpl(
    private val digitalCardRepository: DigitalCardRepository,
    private val menuRepository: MenuRepository
) : DigitalCardService {

    private val log = LoggerFactory.getLogger(DigitalCardServiceImpl::class.java)

    override fun findAll(): List<DigitalCard> {
        val tenantId = requireTenantId()
        log.info("Listing all digital cards for tenant {}", tenantId)
        return digitalCardRepository.findAllByTenantId(tenantId)
    }

    override fun findById(id: UUID): DigitalCard {
        val tenantId = requireTenantId()
        log.info("Finding digital card {} for tenant {}", id, tenantId)
        val card = digitalCardRepository.findById(id)
            ?: throw ResourceNotFoundException(message = "Digital card with id $id not found")
        if (card.tenantId != tenantId) {
            throw ResourceNotFoundException(message = "Digital card with id $id not found")
        }
        return card
    }

    override fun findBySlug(slug: String): DigitalCard {
        log.info("Finding digital card by slug '{}'", slug)
        return digitalCardRepository.findBySlug(slug)
            ?: throw ResourceNotFoundException(
                errorCode = "DIGITAL_CARD_NOT_FOUND",
                message = "Digital card with slug '$slug' not found"
            )
    }

    @Transactional
    override fun create(card: DigitalCard): DigitalCard {
        val tenantId = requireTenantId()
        log.info("Creating digital card with slug '{}' for tenant {}", card.slug, tenantId)

        if (digitalCardRepository.existsBySlug(card.slug)) {
            throw ConflictException(
                errorCode = "SLUG_ALREADY_EXISTS",
                message = "Digital card with slug '${card.slug}' already exists"
            )
        }

        if (!menuRepository.existsById(card.menuId)) {
            throw ResourceNotFoundException(
                errorCode = "MENU_NOT_FOUND",
                message = "Menu with id ${card.menuId} not found"
            )
        }

        val savedCard = digitalCardRepository.save(card)
        log.info("Digital card created with id {}", savedCard.id)
        return savedCard
    }

    @Transactional
    override fun update(id: UUID, card: DigitalCard): DigitalCard {
        val tenantId = requireTenantId()
        log.info("Updating digital card {} for tenant {}", id, tenantId)

        val existing = digitalCardRepository.findById(id)
            ?: throw ResourceNotFoundException(message = "Digital card with id $id not found")
        if (existing.tenantId != tenantId) {
            throw ResourceNotFoundException(message = "Digital card with id $id not found")
        }

        // If slug changed, validate uniqueness
        if (existing.slug != card.slug && digitalCardRepository.existsBySlug(card.slug)) {
            throw ConflictException(
                errorCode = "SLUG_ALREADY_EXISTS",
                message = "Digital card with slug '${card.slug}' already exists"
            )
        }

        existing.slug = card.slug
        existing.customCss = card.customCss
        existing.isActive = card.isActive
        existing.qrCodeUrl = card.qrCodeUrl
        existing.updatedAt = OffsetDateTime.now()

        val savedCard = digitalCardRepository.save(existing)
        log.info("Digital card {} updated", id)
        return savedCard
    }

    @Transactional
    override fun delete(id: UUID) {
        val tenantId = requireTenantId()
        log.info("Deactivating digital card {} for tenant {}", id, tenantId)

        val existing = digitalCardRepository.findById(id)
            ?: throw ResourceNotFoundException(message = "Digital card with id $id not found")
        if (existing.tenantId != tenantId) {
            throw ResourceNotFoundException(message = "Digital card with id $id not found")
        }

        // Soft delete: deactivate instead of hard delete
        existing.isActive = false
        existing.updatedAt = OffsetDateTime.now()
        digitalCardRepository.save(existing)
        log.info("Digital card {} deactivated", id)
    }

    private fun requireTenantId(): UUID {
        val tenantString = TenantContext.getTenant()
            ?: throw BusinessValidationException(
                errorCode = "TENANT_REQUIRED",
                message = "Tenant context is not set. Authentication required."
            )
        return try {
            UUID.fromString(tenantString)
        } catch (e: IllegalArgumentException) {
            throw BusinessValidationException(
                errorCode = "INVALID_TENANT",
                message = "Invalid tenant ID format"
            )
        }
    }
}
