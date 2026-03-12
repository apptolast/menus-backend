package com.apptolast.menus.ingredient.service.impl

import com.apptolast.menus.config.TenantContext
import com.apptolast.menus.ingredient.model.entity.Ingredient
import com.apptolast.menus.ingredient.repository.IngredientRepository
import com.apptolast.menus.ingredient.service.IngredientService
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
class IngredientServiceImpl(
    private val ingredientRepository: IngredientRepository
) : IngredientService {

    private val log = LoggerFactory.getLogger(IngredientServiceImpl::class.java)

    override fun findAll(): List<Ingredient> {
        val tenantId = requireTenantId()
        log.info("Listing all ingredients for tenant {}", tenantId)
        return ingredientRepository.findAllByTenantId(tenantId)
    }

    override fun findById(id: UUID): Ingredient {
        val tenantId = requireTenantId()
        log.info("Finding ingredient {} for tenant {}", id, tenantId)
        val ingredient = ingredientRepository.findById(id)
            ?: throw ResourceNotFoundException(message = "Ingredient with id $id not found")
        if (ingredient.tenantId != tenantId) {
            throw ResourceNotFoundException(message = "Ingredient with id $id not found")
        }
        return ingredient
    }

    @Transactional
    override fun create(ingredient: Ingredient): Ingredient {
        val tenantId = requireTenantId()
        log.info("Creating ingredient '{}' for tenant {}", ingredient.name, tenantId)

        if (ingredientRepository.existsByNameAndTenantId(ingredient.name, tenantId)) {
            throw ConflictException(
                errorCode = "INGREDIENT_ALREADY_EXISTS",
                message = "Ingredient with name '${ingredient.name}' already exists for this tenant"
            )
        }

        return ingredientRepository.save(ingredient)
    }

    @Transactional
    override fun update(id: UUID, ingredient: Ingredient): Ingredient {
        val tenantId = requireTenantId()
        log.info("Updating ingredient {} for tenant {}", id, tenantId)

        val existing = ingredientRepository.findById(id)
            ?: throw ResourceNotFoundException(message = "Ingredient with id $id not found")
        if (existing.tenantId != tenantId) {
            throw ResourceNotFoundException(message = "Ingredient with id $id not found")
        }

        if (existing.name != ingredient.name &&
            ingredientRepository.existsByNameAndTenantId(ingredient.name, tenantId)
        ) {
            throw ConflictException(
                errorCode = "INGREDIENT_ALREADY_EXISTS",
                message = "Ingredient with name '${ingredient.name}' already exists for this tenant"
            )
        }

        existing.name = ingredient.name
        existing.brand = ingredient.brand
        existing.supplier = ingredient.supplier
        existing.allergens = ingredient.allergens
        existing.traces = ingredient.traces
        existing.ocrRawText = ingredient.ocrRawText
        existing.notes = ingredient.notes
        existing.updatedAt = OffsetDateTime.now()

        return ingredientRepository.save(existing)
    }

    @Transactional
    override fun delete(id: UUID) {
        val tenantId = requireTenantId()
        log.info("Deleting ingredient {} for tenant {}", id, tenantId)

        val existing = ingredientRepository.findById(id)
            ?: throw ResourceNotFoundException(message = "Ingredient with id $id not found")
        if (existing.tenantId != tenantId) {
            throw ResourceNotFoundException(message = "Ingredient with id $id not found")
        }

        ingredientRepository.deleteById(id)
    }

    override fun searchByName(name: String): List<Ingredient> {
        val tenantId = requireTenantId()
        log.info("Searching ingredients by name '{}' for tenant {}", name, tenantId)
        return ingredientRepository.searchByName(name, tenantId)
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
