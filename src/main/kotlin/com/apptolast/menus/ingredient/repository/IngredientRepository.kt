package com.apptolast.menus.ingredient.repository

import com.apptolast.menus.ingredient.model.entity.Ingredient
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface IngredientRepository : JpaRepository<Ingredient, UUID> {
    fun existsByName(name: String): Boolean

    @Query("SELECT DISTINCT i FROM Ingredient i LEFT JOIN FETCH i.allergens")
    fun findAllWithAllergens(): List<Ingredient>

    @Query("SELECT i FROM Ingredient i LEFT JOIN FETCH i.allergens WHERE i.id = :id")
    fun findByIdWithAllergens(id: UUID): Optional<Ingredient>

    @Query("SELECT DISTINCT i FROM Ingredient i LEFT JOIN FETCH i.allergens WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    fun searchByName(name: String): List<Ingredient>
}
