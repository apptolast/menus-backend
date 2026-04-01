package com.apptolast.menus.ingredient.service

import com.apptolast.menus.ingredient.dto.request.CreateIngredientRequest
import com.apptolast.menus.ingredient.dto.request.IngredientAllergenRequest
import com.apptolast.menus.ingredient.dto.request.UpdateIngredientRequest
import com.apptolast.menus.ingredient.dto.response.IngredientAllergenResponse
import com.apptolast.menus.ingredient.dto.response.IngredientResponse
import java.util.UUID

interface IngredientService {
    fun findAll(): List<IngredientResponse>
    fun findById(id: UUID): IngredientResponse
    fun create(request: CreateIngredientRequest): IngredientResponse
    fun update(id: UUID, request: UpdateIngredientRequest): IngredientResponse
    fun delete(id: UUID)
    fun searchByName(name: String): List<IngredientResponse>
    fun getAllergens(id: UUID): List<IngredientAllergenResponse>
    fun setAllergens(id: UUID, allergens: List<IngredientAllergenRequest>): IngredientResponse
}
