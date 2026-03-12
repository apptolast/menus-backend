package com.apptolast.menus.recipe.repository

import com.apptolast.menus.recipe.model.entity.Recipe
import com.apptolast.menus.recipe.repository.jpa.JpaRecipeRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class RecipeRepositoryAdapter(
    private val jpa: JpaRecipeRepository
) : RecipeRepository {
    override fun findById(id: UUID): Recipe? = jpa.findById(id).orElse(null)
    override fun findAllByRestaurantId(restaurantId: UUID): List<Recipe> = jpa.findByRestaurantId(restaurantId)
    override fun findActiveByRestaurantId(restaurantId: UUID): List<Recipe> = jpa.findByRestaurantIdAndIsActiveTrue(restaurantId)
    override fun save(entity: Recipe): Recipe = jpa.save(entity)
    override fun deleteById(id: UUID) = jpa.deleteById(id)
    override fun existsById(id: UUID): Boolean = jpa.existsById(id)
}
