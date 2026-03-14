package com.apptolast.menus.recipe.repository

import com.apptolast.menus.recipe.model.entity.Recipe
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface RecipeRepository : JpaRepository<Recipe, UUID> {

    @Query("SELECT DISTINCT r FROM Recipe r LEFT JOIN FETCH r.ingredients ri LEFT JOIN FETCH ri.ingredient WHERE r.restaurantId = :restaurantId")
    fun findByRestaurantIdWithIngredients(restaurantId: UUID): List<Recipe>

    @Query("SELECT DISTINCT r FROM Recipe r LEFT JOIN FETCH r.ingredients ri LEFT JOIN FETCH ri.ingredient WHERE r.restaurantId = :restaurantId AND r.active = true")
    fun findByRestaurantIdAndActiveTrueWithIngredients(restaurantId: UUID): List<Recipe>

    @Query("SELECT r FROM Recipe r LEFT JOIN FETCH r.ingredients ri LEFT JOIN FETCH ri.ingredient WHERE r.id = :id")
    fun findByIdWithIngredients(id: UUID): Optional<Recipe>
}
