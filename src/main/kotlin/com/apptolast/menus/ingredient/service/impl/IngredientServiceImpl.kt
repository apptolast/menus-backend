package com.apptolast.menus.ingredient.service.impl

import com.apptolast.menus.allergen.repository.AllergenRepository
import com.apptolast.menus.dish.model.enum.ContainmentLevel
import com.apptolast.menus.ingredient.dto.request.CreateIngredientRequest
import com.apptolast.menus.ingredient.dto.request.IngredientAllergenRequest
import com.apptolast.menus.ingredient.dto.request.UpdateIngredientRequest
import com.apptolast.menus.ingredient.dto.response.IngredientAllergenResponse
import com.apptolast.menus.ingredient.dto.response.IngredientResponse
import com.apptolast.menus.ingredient.mapper.toAllergenResponse
import com.apptolast.menus.ingredient.mapper.toResponse
import com.apptolast.menus.ingredient.model.entity.Ingredient
import com.apptolast.menus.ingredient.model.entity.IngredientAllergen
import com.apptolast.menus.ingredient.repository.IngredientAllergenRepository
import com.apptolast.menus.ingredient.repository.IngredientRepository
import com.apptolast.menus.ingredient.service.IngredientService
import com.apptolast.menus.shared.exception.ConflictException
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class IngredientServiceImpl(
    private val ingredientRepository: IngredientRepository,
    private val ingredientAllergenRepository: IngredientAllergenRepository,
    private val allergenRepository: AllergenRepository,
    private val entityManager: EntityManager
) : IngredientService {

    private val log = LoggerFactory.getLogger(IngredientServiceImpl::class.java)

    override fun findAll(): List<IngredientResponse> {
        log.info("Listing all ingredients")
        return ingredientRepository.findAllWithAllergens().map { it.toResponse() }
    }

    override fun findById(id: UUID): IngredientResponse {
        log.info("Finding ingredient {}", id)
        return ingredientRepository.findByIdWithAllergens(id)
            .orElseThrow { ResourceNotFoundException(message = "Ingredient with id $id not found") }
            .toResponse()
    }

    @Transactional
    override fun create(request: CreateIngredientRequest): IngredientResponse {
        log.info("Creating ingredient '{}'", request.name)
        if (ingredientRepository.existsByName(request.name)) {
            throw ConflictException(
                errorCode = "INGREDIENT_ALREADY_EXISTS",
                message = "Ingredient with name '${request.name}' already exists"
            )
        }
        val ingredient = Ingredient(
            name = request.name,
            description = request.description,
            brand = request.brand,
            labelInfo = request.labelInfo
        )
        val saved = ingredientRepository.save(ingredient)
        if (request.allergens.isNotEmpty()) {
            saveAllergensBatch(saved, request.allergens)
        }
        return flushClearAndLoad(saved.id)
    }

    @Transactional
    override fun update(id: UUID, request: UpdateIngredientRequest): IngredientResponse {
        log.info("Updating ingredient {}", id)
        val existing = ingredientRepository.findById(id)
            .orElseThrow { ResourceNotFoundException(message = "Ingredient with id $id not found") }

        request.name?.let { existing.name = it }
        request.description?.let { existing.description = it }
        request.brand?.let { existing.brand = it }
        request.labelInfo?.let { existing.labelInfo = it }
        existing.updatedAt = OffsetDateTime.now()
        ingredientRepository.save(existing)

        if (request.allergens != null) {
            // PUT semantics: full replacement
            ingredientAllergenRepository.deleteByIngredientId(id)
            entityManager.flush()

            if (request.allergens.isNotEmpty()) {
                // IngredientAllergen uses UUID PK, so isNew()=true and persist() is used
                saveAllergensBatch(existing, request.allergens)
            }
        }

        return flushClearAndLoad(id)
    }

    @Transactional
    override fun delete(id: UUID) {
        log.info("Deleting ingredient {}", id)
        if (!ingredientRepository.existsById(id)) {
            throw ResourceNotFoundException(message = "Ingredient with id $id not found")
        }
        ingredientAllergenRepository.deleteByIngredientId(id)
        ingredientRepository.deleteById(id)
    }

    override fun searchByName(name: String): List<IngredientResponse> {
        log.info("Searching ingredients by name '{}'", name)
        return ingredientRepository.searchByName(name).map { it.toResponse() }
    }

    override fun getAllergens(id: UUID): List<IngredientAllergenResponse> {
        log.info("Getting allergens for ingredient {}", id)
        if (!ingredientRepository.existsById(id)) {
            throw ResourceNotFoundException(message = "Ingredient with id $id not found")
        }
        return ingredientAllergenRepository.findByIngredientId(id).map { it.toAllergenResponse() }
    }

    @Transactional
    override fun setAllergens(id: UUID, allergens: List<IngredientAllergenRequest>): IngredientResponse {
        log.info("Setting {} allergens for ingredient {}", allergens.size, id)
        ingredientRepository.findById(id)
            .orElseThrow { ResourceNotFoundException(message = "Ingredient with id $id not found") }

        // PUT semantics: full replacement
        ingredientAllergenRepository.deleteByIngredientId(id)
        entityManager.flush()

        if (allergens.isNotEmpty()) {
            val ingredient = ingredientRepository.findById(id)
                .orElseThrow { ResourceNotFoundException(message = "Ingredient with id $id not found") }
            saveAllergensBatch(ingredient, allergens)
            ingredient.updatedAt = OffsetDateTime.now()
            ingredientRepository.save(ingredient)
        }

        return flushClearAndLoad(id)
    }

    private fun flushClearAndLoad(id: UUID): IngredientResponse {
        entityManager.flush()
        entityManager.clear()
        return ingredientRepository.findByIdWithAllergens(id).get().toResponse()
    }

    private fun saveAllergensBatch(ingredient: Ingredient, allergens: List<IngredientAllergenRequest>) {
        // Batch fetch all allergen codes in one query
        val codes = allergens.map { it.allergenCode }
        val allergenEntities = allergenRepository.findAllByCodeIn(codes)
        val allergenMap = allergenEntities.associateBy { it.code }

        val missing = codes.filter { it !in allergenMap }
        if (missing.isNotEmpty()) {
            throw ResourceNotFoundException(message = "Allergens not found: $missing")
        }

        val newAllergens = allergens.map { request ->
            val level = runCatching { ContainmentLevel.valueOf(request.containmentLevel) }
                .getOrElse {
                    throw ResourceNotFoundException(
                        message = "Invalid containment level '${request.containmentLevel}'. Valid values: ${ContainmentLevel.entries.joinToString()}"
                    )
                }
            IngredientAllergen(
                ingredient = ingredient,
                allergen = allergenMap[request.allergenCode]!!,
                containmentLevel = level
            )
        }
        ingredientAllergenRepository.saveAll(newAllergens)
    }
}
