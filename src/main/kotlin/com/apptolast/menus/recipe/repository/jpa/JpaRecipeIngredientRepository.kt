package com.apptolast.menus.recipe.repository.jpa

import com.apptolast.menus.recipe.model.entity.RecipeIngredient
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JpaRecipeIngredientRepository : JpaRepository<RecipeIngredient, UUID> {
    fun findByRecipeId(recipeId: UUID): List<RecipeIngredient>
    fun findBySubRecipeId(subRecipeId: UUID): List<RecipeIngredient>
    fun deleteByRecipeId(recipeId: UUID)
}
