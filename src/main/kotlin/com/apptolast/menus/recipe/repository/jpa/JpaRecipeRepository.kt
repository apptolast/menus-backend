package com.apptolast.menus.recipe.repository.jpa

import com.apptolast.menus.recipe.model.entity.Recipe
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface JpaRecipeRepository : JpaRepository<Recipe, UUID> {
    fun findByRestaurantId(restaurantId: UUID): List<Recipe>
    fun findByRestaurantIdAndIsActiveTrue(restaurantId: UUID): List<Recipe>

    @Query("SELECT r FROM Recipe r LEFT JOIN FETCH r.ingredients WHERE r.id = :id")
    fun findByIdWithIngredients(@Param("id") id: UUID): Recipe?
}
