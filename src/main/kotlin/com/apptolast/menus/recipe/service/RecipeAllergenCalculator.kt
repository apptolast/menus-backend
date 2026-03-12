package com.apptolast.menus.recipe.service

import java.util.UUID

interface RecipeAllergenCalculator {
    fun computeAllergens(recipeId: UUID): List<ComputedAllergen>
    fun detectCycle(recipeId: UUID, newSubRecipeId: UUID): Boolean
}

data class ComputedAllergen(
    val code: String,
    val level: String,
    val sources: List<String>
)
