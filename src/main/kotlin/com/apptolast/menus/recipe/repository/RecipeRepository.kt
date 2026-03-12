package com.apptolast.menus.recipe.repository

import com.apptolast.menus.recipe.model.entity.Recipe
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RecipeRepository : JpaRepository<Recipe, UUID> {
    fun findByRestaurantId(restaurantId: UUID): List<Recipe>
    fun findByRestaurantIdAndActiveTrue(restaurantId: UUID): List<Recipe>
}
