package com.apptolast.menus.recipe.repository

import com.apptolast.menus.recipe.model.entity.RecipeIngredient
import com.apptolast.menus.recipe.model.entity.RecipeIngredientId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface RecipeIngredientRepository : JpaRepository<RecipeIngredient, RecipeIngredientId> {

    @Query("SELECT ri FROM RecipeIngredient ri JOIN FETCH ri.ingredient WHERE ri.recipe.id = :recipeId")
    fun findByRecipeIdWithIngredient(recipeId: UUID): List<RecipeIngredient>

    fun deleteByRecipeId(recipeId: UUID)
}
