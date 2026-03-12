package com.apptolast.menus.recipe.repository

import com.apptolast.menus.recipe.model.entity.Recipe
import java.util.UUID

interface RecipeRepository {
    fun findById(id: UUID): Recipe?
    fun findAllByRestaurantId(restaurantId: UUID): List<Recipe>
    fun findActiveByRestaurantId(restaurantId: UUID): List<Recipe>
    fun save(entity: Recipe): Recipe
    fun deleteById(id: UUID)
    fun existsById(id: UUID): Boolean
}
