package com.apptolast.menus.ingredient.repository

import com.apptolast.menus.ingredient.model.entity.IngredientAllergen
import com.apptolast.menus.ingredient.model.entity.IngredientAllergenId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface IngredientAllergenRepository : JpaRepository<IngredientAllergen, IngredientAllergenId> {

    @Query("SELECT ia FROM IngredientAllergen ia JOIN FETCH ia.allergen WHERE ia.ingredient.id = :ingredientId")
    fun findByIngredientId(@Param("ingredientId") ingredientId: UUID): List<IngredientAllergen>

    @Modifying
    @Query("DELETE FROM IngredientAllergen ia WHERE ia.ingredient.id = :ingredientId")
    fun deleteByIngredientId(@Param("ingredientId") ingredientId: UUID)

    @Modifying
    @Query("DELETE FROM IngredientAllergen ia WHERE ia.ingredient.id = :ingredientId AND ia.allergen.id = :allergenId")
    fun deleteByIngredientIdAndAllergenId(
        @Param("ingredientId") ingredientId: UUID,
        @Param("allergenId") allergenId: Int
    )
}
