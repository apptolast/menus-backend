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

        // Batch: collect ALL ingredient IDs across ALL recipes, single query for allergens
        val allIngredientIds = recipes.flatMap { it.ingredients.map { ri -> ri.ingredient.id } }.distinct()
        val allergensByIngredient = if (allIngredientIds.isNotEmpty()) {
            ingredientAllergenRepository.findByIngredientIdIn(allIngredientIds)
                .groupBy { it.ingredient.id }
        } else emptyMap()

        return recipes.map { recipe ->
            val recipeIngredientIds = recipe.ingredients.map { it.ingredient.id }
            val allergenCount = computeAllergensFromMap(recipeIngredientIds, allergensByIngredient).size
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

        if (request.ingredients.isNotEmpty()) {
            // Batch fetch all ingredients in one query
            val ingredientIds = request.ingredients.map { it.ingredientId }
            val ingredients = ingredientRepository.findAllById(ingredientIds)
            if (ingredients.size != ingredientIds.size) {
                val found = ingredients.map { it.id }.toSet()
                val missing = ingredientIds.filter { it !in found }
                throw ResourceNotFoundException(message = "Ingredients not found: $missing")
            }
            val ingredientMap = ingredients.associateBy { it.id }

            val recipeIngredients = request.ingredients.map { input ->
                RecipeIngredient(
                    recipe = savedRecipe,
                    ingredient = ingredientMap[input.ingredientId]!!,
                    quantity = input.quantity,
                    unit = input.unit
                )
            }
            recipeIngredientRepository.saveAll(recipeIngredients)
        }

        log.info("Recipe '{}' created with id {}", savedRecipe.name, savedRecipe.id)
        return flushClearAndLoad(savedRecipe.id)
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
            // PUT semantics: full replacement
            recipeIngredientRepository.deleteByRecipeId(id)
            entityManager.flush()

            if (request.ingredients.isNotEmpty()) {
                // Batch fetch all ingredients in one query
                val ingredientIds = request.ingredients.map { it.ingredientId }
                val ingredients = ingredientRepository.findAllById(ingredientIds)
                if (ingredients.size != ingredientIds.size) {
                    val found = ingredients.map { it.id }.toSet()
                    val missing = ingredientIds.filter { it !in found }
                    throw ResourceNotFoundException(message = "Ingredients not found: $missing")
                }
                val ingredientMap = ingredients.associateBy { it.id }

                // RecipeIngredient uses UUID PK, so isNew()=true and persist() is used
                val newIngredients = request.ingredients.map { input ->
                    RecipeIngredient(
                        recipe = existing,
                        ingredient = ingredientMap[input.ingredientId]!!,
                        quantity = input.quantity,
                        unit = input.unit
                    )
                }
                recipeIngredientRepository.saveAll(newIngredients)
            }
        }

        return flushClearAndLoad(id)
    }

    override fun computeAllergens(recipeId: UUID): List<ComputedAllergen> {
        log.debug("Computing allergens for recipe {}", recipeId)
        val recipeIngredients = recipeIngredientRepository.findByRecipeIdWithIngredient(recipeId)
        val ingredientIds = recipeIngredients.map { it.ingredient.id }

        if (ingredientIds.isEmpty()) return emptyList()

        // Batch: single query for all ingredient allergens
        val allergensByIngredient = ingredientAllergenRepository.findByIngredientIdIn(ingredientIds)
            .groupBy { it.ingredient.id }

        return computeAllergensFromMap(ingredientIds, allergensByIngredient)
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

    private fun flushClearAndLoad(id: UUID): RecipeResponse {
        entityManager.flush()
        entityManager.clear()
        val recipe = recipeRepository.findByIdWithIngredients(id)
            .orElseThrow { ResourceNotFoundException(message = "Recipe with id $id not found") }
        val allergens = computeAllergens(recipe.id)
        return recipe.toResponse(allergens)
    }

    private fun computeAllergensFromMap(
        ingredientIds: List<UUID>,
        allergensByIngredient: Map<UUID, List<com.apptolast.menus.ingredient.model.entity.IngredientAllergen>>
    ): List<ComputedAllergen> {
        data class AllergenEntry(val id: Int, val code: String, val name: String, val level: ContainmentLevel)
        val allergenMap = mutableMapOf<String, AllergenEntry>()

        for (ingredientId in ingredientIds) {
            val ingredientAllergens = allergensByIngredient[ingredientId] ?: continue
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
}
