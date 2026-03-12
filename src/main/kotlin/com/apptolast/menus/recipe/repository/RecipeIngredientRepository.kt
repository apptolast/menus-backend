package com.apptolast.menus.recipe.repository

import com.apptolast.menus.recipe.model.entity.RecipeIngredient
import java.util.UUID

interface RecipeIngredientRepository {
    fun findByRecipeId(recipeId: UUID): List<RecipeIngredient>
    fun findBySubRecipeId(subRecipeId: UUID): List<RecipeIngredient>
    fun saveAll(entities: List<RecipeIngredient>): List<RecipeIngredient>
    fun deleteByRecipeId(recipeId: UUID)
}
