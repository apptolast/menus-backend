package com.apptolast.menus.recipe.service.impl

import com.apptolast.menus.dish.model.enum.ContainmentLevel
import com.apptolast.menus.ingredient.repository.IngredientAllergenRepository
import com.apptolast.menus.ingredient.repository.IngredientRepository
import com.apptolast.menus.recipe.model.entity.Recipe
import com.apptolast.menus.recipe.model.entity.RecipeIngredient
import com.apptolast.menus.recipe.repository.RecipeIngredientRepository
import com.apptolast.menus.recipe.repository.RecipeRepository
import com.apptolast.menus.recipe.service.ComputedAllergen
import com.apptolast.menus.recipe.service.RecipeIngredientInput
import com.apptolast.menus.recipe.service.RecipeService
import com.apptolast.menus.shared.exception.ResourceNotFoundException
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class RecipeServiceImpl(
    private val recipeRepository: RecipeRepository,
    private val recipeIngredientRepository: RecipeIngredientRepository,
    private val ingredientRepository: IngredientRepository,
    private val ingredientAllergenRepository: IngredientAllergenRepository,
    private val entityManager: EntityManager
) : RecipeService {

    private val log = LoggerFactory.getLogger(RecipeServiceImpl::class.java)

    override fun findAllByRestaurant(restaurantId: UUID): List<Recipe> {
        log.info("Listing recipes for restaurant {}", restaurantId)
        return recipeRepository.findByRestaurantIdWithIngredients(restaurantId)
    }

    override fun findById(id: UUID): Recipe {
        log.info("Finding recipe {}", id)
        return recipeRepository.findByIdWithIngredients(id)
            .orElseThrow { ResourceNotFoundException(message = "Recipe with id $id not found") }
    }

    @Transactional
    override fun create(recipe: Recipe, ingredientInputs: List<RecipeIngredientInput>): Recipe {
        log.info("Creating recipe '{}'", recipe.name)
        val savedRecipe = recipeRepository.save(recipe)

        val recipeIngredients = ingredientInputs.map { input ->
            val ingredient = ingredientRepository.findById(input.ingredientId)
                .orElseThrow { ResourceNotFoundException(message = "Ingredient with id ${input.ingredientId} not found") }
            RecipeIngredient(
                recipe = savedRecipe,
                ingredient = ingredient,
                quantity = input.quantity,
                unit = input.unit
            )
        }
        if (recipeIngredients.isNotEmpty()) {
            recipeIngredientRepository.saveAll(recipeIngredients)
        }
        entityManager.flush()
        entityManager.clear()
        log.info("Recipe '{}' created with id {}", savedRecipe.name, savedRecipe.id)
        return recipeRepository.findByIdWithIngredients(savedRecipe.id)
            .orElseThrow { ResourceNotFoundException(message = "Recipe with id ${savedRecipe.id} not found") }
    }

    @Transactional
    override fun update(id: UUID, recipe: Recipe, ingredientInputs: List<RecipeIngredientInput>?): Recipe {
        log.info("Updating recipe {}", id)
        val existing = recipeRepository.findById(id)
            .orElseThrow { ResourceNotFoundException(message = "Recipe with id $id not found") }

        existing.name = recipe.name
        existing.description = recipe.description
        existing.category = recipe.category
        existing.active = recipe.active
        existing.updatedAt = OffsetDateTime.now()
        recipeRepository.save(existing)

        if (!ingredientInputs.isNullOrEmpty()) {
            val currentIngredients = recipeIngredientRepository.findByRecipeIdWithIngredient(id)
            val currentIngredientIds = currentIngredients.map { it.ingredient.id }.toSet()

            for (input in ingredientInputs) {
                if (input.ingredientId in currentIngredientIds) {
                    val current = currentIngredients.first { it.ingredient.id == input.ingredientId }
                    current.quantity = input.quantity
                    current.unit = input.unit
                    recipeIngredientRepository.save(current)
                } else {
                    val ingredient = ingredientRepository.findById(input.ingredientId)
                        .orElseThrow { ResourceNotFoundException(message = "Ingredient with id ${input.ingredientId} not found") }
                    recipeIngredientRepository.save(
                        RecipeIngredient(
                            recipe = existing,
                            ingredient = ingredient,
                            quantity = input.quantity,
                            unit = input.unit
                        )
                    )
                }
            }
        }

        entityManager.flush()
        entityManager.clear()
        return recipeRepository.findByIdWithIngredients(id)
            .orElseThrow { ResourceNotFoundException(message = "Recipe with id $id not found") }
    }

    override fun computeAllergens(recipeId: UUID): List<ComputedAllergen> {
        log.info("Computing allergens for recipe {}", recipeId)
        val recipeIngredients = recipeIngredientRepository.findByRecipeIdWithIngredient(recipeId)

        // Map: allergenCode -> Pair(allergen entity, highest containment level)
        data class AllergenEntry(val id: Int, val code: String, val name: String, val level: ContainmentLevel)
        val allergenMap = mutableMapOf<String, AllergenEntry>()

        for (ri in recipeIngredients) {
            val ingredientAllergens = ingredientAllergenRepository.findByIngredientId(ri.ingredient.id)
            for (ia in ingredientAllergens) {
                val code = ia.allergen.code
                val existing = allergenMap[code]
                // CONTAINS takes priority over MAY_CONTAIN
                if (existing == null || ia.containmentLevel == ContainmentLevel.CONTAINS) {
                    allergenMap[code] = AllergenEntry(
                        id = ia.allergen.id,
                        code = ia.allergen.code,
                        name = ia.allergen.nameEs,
                        level = ia.containmentLevel
                    )
                }
            }
        }

        return allergenMap.values.map { entry ->
            ComputedAllergen(
                allergenId = entry.id,
                allergenCode = entry.code,
                allergenName = entry.name,
                containmentLevel = entry.level.name
            )
        }
    }

    @Transactional
    override fun delete(id: UUID) {
        log.info("Deleting recipe {}", id)
        if (!recipeRepository.existsById(id)) {
            throw ResourceNotFoundException(message = "Recipe with id $id not found")
        }
        recipeIngredientRepository.deleteByRecipeId(id)
        recipeRepository.deleteById(id)
    }
}
