package com.apptolast.menus.recipe.service.impl

import com.apptolast.menus.config.TenantContext
import com.apptolast.menus.ingredient.repository.IngredientRepository
import com.apptolast.menus.menu.repository.MenuRecipeRepository
import com.apptolast.menus.recipe.model.entity.Recipe
import com.apptolast.menus.recipe.model.entity.RecipeIngredient
import com.apptolast.menus.recipe.repository.RecipeIngredientRepository
import com.apptolast.menus.recipe.repository.RecipeRepository
import com.apptolast.menus.recipe.service.RecipeAllergenCalculator
import com.apptolast.menus.recipe.service.RecipeIngredientInput
import com.apptolast.menus.recipe.service.RecipeService
import com.apptolast.menus.shared.exception.BusinessValidationException
import com.apptolast.menus.shared.exception.CyclicRecipeException
import com.apptolast.menus.shared.exception.ResourceNotFoundException
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
    private val recipeAllergenCalculator: RecipeAllergenCalculator,
    private val menuRecipeRepository: MenuRecipeRepository
) : RecipeService {

    private val log = LoggerFactory.getLogger(RecipeServiceImpl::class.java)

    override fun findAllByRestaurant(restaurantId: UUID): List<Recipe> {
        log.info("Listing recipes for restaurant {}", restaurantId)
        return recipeRepository.findAllByRestaurantId(restaurantId)
    }

    override fun findById(id: UUID): Recipe {
        log.info("Finding recipe {}", id)
        return recipeRepository.findById(id)
            ?: throw ResourceNotFoundException(message = "Recipe with id $id not found")
    }

    @Transactional
    override fun create(recipe: Recipe, ingredientInputs: List<RecipeIngredientInput>): Recipe {
        val tenantId = requireTenantId()
        log.info("Creating recipe '{}' for tenant {}", recipe.name, tenantId)

        validateIngredientInputs(ingredientInputs)

        // Check for cycles if any sub-recipes are referenced
        for (input in ingredientInputs) {
            val subRecipeId = input.subRecipeId
            if (subRecipeId != null) {
                if (!recipeRepository.existsById(subRecipeId)) {
                    throw ResourceNotFoundException(message = "Sub-recipe with id $subRecipeId not found")
                }
                if (recipeAllergenCalculator.detectCycle(recipe.id, subRecipeId)) {
                    throw CyclicRecipeException(
                        "Adding sub-recipe $subRecipeId to recipe ${recipe.id} would create a cycle"
                    )
                }
            }
        }

        val savedRecipe = recipeRepository.save(recipe)

        val recipeIngredients = ingredientInputs.map { input ->
            RecipeIngredient(
                recipe = savedRecipe,
                ingredient = input.ingredientId?.let { ingredientId ->
                    ingredientRepository.findById(ingredientId)
                        ?: throw ResourceNotFoundException(message = "Ingredient with id $ingredientId not found")
                },
                subRecipe = input.subRecipeId?.let { subId ->
                    recipeRepository.findById(subId)
                        ?: throw ResourceNotFoundException(message = "Sub-recipe with id $subId not found")
                },
                tenantId = tenantId,
                quantity = input.quantity,
                unit = input.unit,
                notes = input.notes,
                sortOrder = input.sortOrder
            )
        }

        if (recipeIngredients.isNotEmpty()) {
            recipeIngredientRepository.saveAll(recipeIngredients)
        }

        log.info("Recipe '{}' created with id {}", savedRecipe.name, savedRecipe.id)
        return savedRecipe
    }

    @Transactional
    override fun update(id: UUID, recipe: Recipe, ingredientInputs: List<RecipeIngredientInput>?): Recipe {
        val tenantId = requireTenantId()
        log.info("Updating recipe {} for tenant {}", id, tenantId)

        val existing = recipeRepository.findById(id)
            ?: throw ResourceNotFoundException(message = "Recipe with id $id not found")
        if (existing.tenantId != tenantId) {
            throw ResourceNotFoundException(message = "Recipe with id $id not found")
        }

        existing.name = recipe.name
        existing.description = recipe.description
        existing.category = recipe.category
        existing.isSubElaboration = recipe.isSubElaboration
        existing.price = recipe.price
        existing.imageUrl = recipe.imageUrl
        existing.isActive = recipe.isActive
        existing.updatedAt = OffsetDateTime.now()

        if (ingredientInputs != null) {
            validateIngredientInputs(ingredientInputs)

            for (input in ingredientInputs) {
                val subRecipeId = input.subRecipeId
                if (subRecipeId != null) {
                    if (!recipeRepository.existsById(subRecipeId)) {
                        throw ResourceNotFoundException(message = "Sub-recipe with id $subRecipeId not found")
                    }
                    if (recipeAllergenCalculator.detectCycle(id, subRecipeId)) {
                        throw CyclicRecipeException(
                            "Adding sub-recipe $subRecipeId to recipe $id would create a cycle"
                        )
                    }
                }
            }

            recipeIngredientRepository.deleteByRecipeId(id)

            val recipeIngredients = ingredientInputs.map { input ->
                RecipeIngredient(
                    recipe = existing,
                    ingredient = input.ingredientId?.let { ingredientId ->
                        ingredientRepository.findById(ingredientId)
                            ?: throw ResourceNotFoundException(message = "Ingredient with id $ingredientId not found")
                    },
                    subRecipe = input.subRecipeId?.let { subId ->
                        recipeRepository.findById(subId)
                            ?: throw ResourceNotFoundException(message = "Sub-recipe with id $subId not found")
                    },
                    tenantId = tenantId,
                    quantity = input.quantity,
                    unit = input.unit,
                    notes = input.notes,
                    sortOrder = input.sortOrder
                )
            }

            if (recipeIngredients.isNotEmpty()) {
                recipeIngredientRepository.saveAll(recipeIngredients)
            }
        }

        val savedRecipe = recipeRepository.save(existing)
        log.info("Recipe {} updated", id)
        return savedRecipe
    }

    @Transactional
    override fun delete(id: UUID) {
        val tenantId = requireTenantId()
        log.info("Deleting recipe {} for tenant {}", id, tenantId)

        val existing = recipeRepository.findById(id)
            ?: throw ResourceNotFoundException(message = "Recipe with id $id not found")
        if (existing.tenantId != tenantId) {
            throw ResourceNotFoundException(message = "Recipe with id $id not found")
        }

        // If referenced by menus, soft-delete; otherwise hard delete
        val menuReferences = menuRecipeRepository.findByRecipeId(id)
        if (menuReferences.isNotEmpty()) {
            log.info("Recipe {} is referenced by {} menus, performing soft delete", id, menuReferences.size)
            existing.isActive = false
            existing.updatedAt = OffsetDateTime.now()
            recipeRepository.save(existing)
        } else {
            log.info("Recipe {} has no menu references, performing hard delete", id)
            recipeIngredientRepository.deleteByRecipeId(id)
            recipeRepository.deleteById(id)
        }
    }

    private fun validateIngredientInputs(inputs: List<RecipeIngredientInput>) {
        for ((index, input) in inputs.withIndex()) {
            val hasIngredient = input.ingredientId != null
            val hasSubRecipe = input.subRecipeId != null

            if (hasIngredient == hasSubRecipe) {
                throw BusinessValidationException(
                    errorCode = "INVALID_RECIPE_INGREDIENT",
                    message = "Recipe ingredient at index $index must have exactly one of ingredientId or subRecipeId"
                )
            }
        }
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
