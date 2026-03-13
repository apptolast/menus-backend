package com.apptolast.menus.ingredient.service.impl

import com.apptolast.menus.allergen.repository.AllergenRepository
import com.apptolast.menus.dish.model.enum.ContainmentLevel
import com.apptolast.menus.ingredient.dto.request.IngredientAllergenRequest
import com.apptolast.menus.ingredient.dto.response.IngredientAllergenResponse
import com.apptolast.menus.ingredient.mapper.toAllergenResponse
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

    override fun findAll(): List<Ingredient> {
        log.info("Listing all ingredients")
        return ingredientRepository.findAllWithAllergens()
    }

    override fun findById(id: UUID): Ingredient {
        log.info("Finding ingredient {}", id)
        return ingredientRepository.findByIdWithAllergens(id)
            .orElseThrow { ResourceNotFoundException(message = "Ingredient with id $id not found") }
    }

    @Transactional
    override fun create(ingredient: Ingredient, allergens: List<IngredientAllergenRequest>): Ingredient {
        log.info("Creating ingredient '{}'", ingredient.name)
        if (ingredientRepository.existsByName(ingredient.name)) {
            throw ConflictException(
                errorCode = "INGREDIENT_ALREADY_EXISTS",
                message = "Ingredient with name '${ingredient.name}' already exists"
            )
        }
        val saved = ingredientRepository.save(ingredient)
        if (allergens.isNotEmpty()) {
            saveAllergens(saved, allergens)
        }
        entityManager.flush()
        entityManager.clear()
        return ingredientRepository.findByIdWithAllergens(saved.id).get()
    }

    @Transactional
    override fun update(id: UUID, ingredient: Ingredient, allergens: List<IngredientAllergenRequest>?): Ingredient {
        log.info("Updating ingredient {}", id)
        val existing = ingredientRepository.findById(id)
            .orElseThrow { ResourceNotFoundException(message = "Ingredient with id $id not found") }
        existing.name = ingredient.name
        existing.description = ingredient.description
        existing.brand = ingredient.brand
        existing.labelInfo = ingredient.labelInfo
        existing.updatedAt = OffsetDateTime.now()

        if (allergens != null) {
            ingredientAllergenRepository.deleteByIngredientId(id)
            if (allergens.isNotEmpty()) {
                saveAllergens(existing, allergens)
            }
        }

        ingredientRepository.save(existing)
        entityManager.flush()
        entityManager.clear()
        return ingredientRepository.findByIdWithAllergens(id).get()
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

    override fun searchByName(name: String): List<Ingredient> {
        log.info("Searching ingredients by name '{}'", name)
        return ingredientRepository.searchByName(name)
    }

    override fun getAllergens(id: UUID): List<IngredientAllergenResponse> {
        log.info("Getting allergens for ingredient {}", id)
        if (!ingredientRepository.existsById(id)) {
            throw ResourceNotFoundException(message = "Ingredient with id $id not found")
        }
        return ingredientAllergenRepository.findByIngredientId(id).map { it.toAllergenResponse() }
    }

    @Transactional
    override fun setAllergens(id: UUID, allergens: List<IngredientAllergenRequest>): Ingredient {
        log.info("Setting {} allergens for ingredient {}", allergens.size, id)
        val ingredient = ingredientRepository.findById(id)
            .orElseThrow { ResourceNotFoundException(message = "Ingredient with id $id not found") }

        ingredientAllergenRepository.deleteByIngredientId(id)
        if (allergens.isNotEmpty()) {
            saveAllergens(ingredient, allergens)
        }

        ingredient.updatedAt = OffsetDateTime.now()
        ingredientRepository.save(ingredient)
        entityManager.flush()
        entityManager.clear()
        return ingredientRepository.findByIdWithAllergens(id).get()
    }

    private fun saveAllergens(ingredient: Ingredient, allergens: List<IngredientAllergenRequest>) {
        val newAllergens = allergens.map { request ->
            val allergen = allergenRepository.findByCode(request.allergenCode)
                ?: throw ResourceNotFoundException(message = "Allergen with code '${request.allergenCode}' not found")
            val level = runCatching { ContainmentLevel.valueOf(request.containmentLevel) }
                .getOrElse {
                    throw ResourceNotFoundException(
                        message = "Invalid containment level '${request.containmentLevel}'. Valid values: ${ContainmentLevel.values().joinToString()}"
                    )
                }
            IngredientAllergen(ingredient = ingredient, allergen = allergen, containmentLevel = level)
        }
        ingredientAllergenRepository.saveAll(newAllergens)
    }
}
