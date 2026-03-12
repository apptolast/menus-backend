package com.apptolast.menus.recipe.service

import com.apptolast.menus.recipe.model.entity.Recipe
import java.math.BigDecimal
import java.util.UUID

interface RecipeService {
    fun findAllByRestaurant(restaurantId: UUID): List<Recipe>
    fun findById(id: UUID): Recipe
    fun create(recipe: Recipe, ingredientInputs: List<RecipeIngredientInput>): Recipe
    fun update(id: UUID, recipe: Recipe, ingredientInputs: List<RecipeIngredientInput>?): Recipe
    fun delete(id: UUID)
    fun computeAllergens(recipeId: UUID): List<ComputedAllergen>
}

data class RecipeIngredientInput(
    val ingredientId: UUID,
    val quantity: BigDecimal? = null,
    val unit: String? = null
)

data class ComputedAllergen(
    val allergenId: Int,
    val allergenCode: String,
    val allergenName: String,
    val containmentLevel: String
)
