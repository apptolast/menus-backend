package com.apptolast.menus.recipe.repository

import com.apptolast.menus.recipe.model.entity.RecipeIngredient
import com.apptolast.menus.recipe.repository.jpa.JpaRecipeIngredientRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class RecipeIngredientRepositoryAdapter(
    private val jpa: JpaRecipeIngredientRepository
) : RecipeIngredientRepository {
    override fun findByRecipeId(recipeId: UUID): List<RecipeIngredient> = jpa.findByRecipeId(recipeId)
    override fun findBySubRecipeId(subRecipeId: UUID): List<RecipeIngredient> = jpa.findBySubRecipeId(subRecipeId)
    override fun saveAll(entities: List<RecipeIngredient>): List<RecipeIngredient> = jpa.saveAll(entities)
    override fun deleteByRecipeId(recipeId: UUID) = jpa.deleteByRecipeId(recipeId)
}
