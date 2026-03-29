package com.apptolast.menus.ingredient.service

import com.apptolast.menus.ingredient.dto.request.IngredientAllergenRequest
import com.apptolast.menus.ingredient.dto.response.IngredientAllergenResponse
import com.apptolast.menus.ingredient.model.entity.Ingredient
import java.util.UUID

interface IngredientService {
    fun findAll(): List<Ingredient>
    fun findById(id: UUID): Ingredient
    fun create(ingredient: Ingredient, allergens: List<IngredientAllergenRequest> = emptyList()): Ingredient
    fun update(id: UUID, ingredient: Ingredient, allergens: List<IngredientAllergenRequest>? = null): Ingredient
    fun delete(id: UUID)
    fun searchByName(name: String): List<Ingredient>
    fun getAllergens(id: UUID): List<IngredientAllergenResponse>
    fun setAllergens(id: UUID, allergens: List<IngredientAllergenRequest>): Ingredient
}
