package com.apptolast.menus.recipe.service.impl

import com.apptolast.menus.dish.model.enum.ContainmentLevel
import com.apptolast.menus.ingredient.repository.IngredientAllergenRepository
import com.apptolast.menus.ingredient.repository.IngredientRepository
import com.apptolast.menus.recipe.dto.request.CreateRecipeRequest
import com.apptolast.menus.recipe.dto.request.UpdateRecipeRequest
import com.apptolast.menus.recipe.dto.response.RecipeResponse
import com.apptolast.menus.recipe.dto.response.RecipeSummaryResponse
import com.apptolast.menus.recipe.mapper.toResponse
import com.apptolast.menus.recipe.mapper.toSummaryResponse
import com.apptolast.menus.recipe.model.entity.Recipe
import com.apptolast.menus.recipe.model.entity.RecipeIngredient
import com.apptolast.menus.recipe.repository.RecipeIngredientRepository
import com.apptolast.menus.recipe.repository.RecipeRepository
import com.apptolast.menus.recipe.service.ComputedAllergen
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

    override fun findAllByRestaurant(restaurantId: UUID): List<RecipeSummaryResponse> {
        log.info("Listing recipes for restaurant {}", restaurantId)
        val recipes = recipeRepository.findByRestaurantIdWithIngredients(restaurantId)
        return recipes.map { recipe ->
            val allergenCount = computeAllergens(recipe.id).size
            recipe.toSummaryResponse(allergenCount)
        }
    }

    override fun findById(id: UUID): RecipeResponse {
        log.info("Finding recipe {}", id)
        val recipe = recipeRepository.findByIdWithIngredients(id)
            .orElseThrow { ResourceNotFoundException(message = "Recipe with id $id not found") }
        val allergens = computeAllergens(recipe.id)
        return recipe.toResponse(allergens)
    }

    @Transactional
    override fun create(request: CreateRecipeRequest): RecipeResponse {
        log.info("Creating recipe '{}'", request.name)
        val recipe = Recipe(
            restaurantId = request.restaurantId,
            name = request.name,
            description = request.description,
            category = request.category,
            price = request.price
        )
        val savedRecipe = recipeRepository.save(recipe)

        val recipeIngredients = request.ingredients.map { input ->
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
        val saved = recipeRepository.findByIdWithIngredients(savedRecipe.id)
            .orElseThrow { ResourceNotFoundException(message = "Recipe with id ${savedRecipe.id} not found") }
        val allergens = computeAllergens(saved.id)
        return saved.toResponse(allergens)
    }

    @Transactional
    override fun update(id: UUID, request: UpdateRecipeRequest): RecipeResponse {
        log.info("Updating recipe {}", id)
        val existing = recipeRepository.findById(id)
            .orElseThrow { ResourceNotFoundException(message = "Recipe with id $id not found") }

        request.name?.let { existing.name = it }
        request.description?.let { existing.description = it }
        request.category?.let { existing.category = it }
        request.price?.let { existing.price = it }
        request.active?.let { existing.active = it }
        existing.updatedAt = OffsetDateTime.now()
        recipeRepository.save(existing)

        if (request.ingredients != null) {
            // PUT semantics: full replacement of ingredients
            recipeIngredientRepository.deleteByRecipeId(id)

            // Flush DELETE to DB and clear stale entities from JPA cache.
            // Without this, saveAll() calls merge() on stale cached RecipeIngredient
            // (same composite key) instead of persist(), silently losing data.
            entityManager.flush()
            entityManager.clear()

            if (request.ingredients.isNotEmpty()) {
                // Re-fetch recipe after clear (detached by entityManager.clear())
                val freshRecipe = recipeRepository.findById(id)
                    .orElseThrow { ResourceNotFoundException(message = "Recipe with id $id not found") }
                val newIngredients = request.ingredients.map { input ->
                    val ingredient = ingredientRepository.findById(input.ingredientId)
                        .orElseThrow { ResourceNotFoundException(message = "Ingredient with id ${input.ingredientId} not found") }
                    RecipeIngredient(
                        recipe = freshRecipe,
                        ingredient = ingredient,
                        quantity = input.quantity,
                        unit = input.unit
                    )
                }
                recipeIngredientRepository.saveAll(newIngredients)
            }
        }

        entityManager.flush()
        entityManager.clear()
        val updated = recipeRepository.findByIdWithIngredients(id)
            .orElseThrow { ResourceNotFoundException(message = "Recipe with id $id not found") }
        val allergens = computeAllergens(updated.id)
        return updated.toResponse(allergens)
    }

    override fun computeAllergens(recipeId: UUID): List<ComputedAllergen> {
        log.info("Computing allergens for recipe {}", recipeId)
        val recipeIngredients = recipeIngredientRepository.findByRecipeIdWithIngredient(recipeId)

        data class AllergenEntry(val id: Int, val code: String, val name: String, val level: ContainmentLevel)
        val allergenMap = mutableMapOf<String, AllergenEntry>()

        for (ri in recipeIngredients) {
            val ingredientAllergens = ingredientAllergenRepository.findByIngredientId(ri.ingredient.id)
            for (ia in ingredientAllergens) {
                val code = ia.allergen.code
                val existing = allergenMap[code]
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
