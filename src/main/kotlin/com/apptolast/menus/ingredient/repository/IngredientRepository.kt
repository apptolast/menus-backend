package com.apptolast.menus.ingredient.repository

import com.apptolast.menus.ingredient.model.entity.Ingredient
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface IngredientRepository : JpaRepository<Ingredient, UUID> {
    fun existsByName(name: String): Boolean
    @Query("SELECT i FROM Ingredient i WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    fun searchByName(name: String): List<Ingredient>
}
