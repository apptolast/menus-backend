package com.apptolast.menus.recipe.repository

import com.apptolast.menus.recipe.model.entity.RecipeIngredient
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface RecipeIngredientRepository : JpaRepository<RecipeIngredient, UUID> {

    @Query("SELECT ri FROM RecipeIngredient ri JOIN FETCH ri.ingredient WHERE ri.recipe.id = :recipeId")
    fun findByRecipeIdWithIngredient(recipeId: UUID): List<RecipeIngredient>

    @Modifying
    @Query("DELETE FROM RecipeIngredient ri WHERE ri.recipe.id = :recipeId")
    fun deleteByRecipeId(recipeId: UUID)
}
