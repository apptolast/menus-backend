package com.apptolast.menus.recipe.repository

import com.apptolast.menus.recipe.model.entity.RecipeIngredient
import com.apptolast.menus.recipe.model.entity.RecipeIngredientId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RecipeIngredientRepository : JpaRepository<RecipeIngredient, RecipeIngredientId> {
    fun findByRecipeId(recipeId: UUID): List<RecipeIngredient>
    fun deleteByRecipeId(recipeId: UUID)
}
