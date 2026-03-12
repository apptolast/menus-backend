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
}

data class RecipeIngredientInput(
    val ingredientId: UUID? = null,
    val subRecipeId: UUID? = null,
    val quantity: BigDecimal? = null,
    val unit: String? = null,
    val notes: String? = null,
    val sortOrder: Int = 0
)
