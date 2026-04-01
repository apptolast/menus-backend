package com.apptolast.menus.recipe.service

import com.apptolast.menus.recipe.dto.request.CreateRecipeRequest
import com.apptolast.menus.recipe.dto.request.UpdateRecipeRequest
import com.apptolast.menus.recipe.dto.response.RecipeResponse
import com.apptolast.menus.recipe.dto.response.RecipeSummaryResponse
import java.util.UUID

interface RecipeService {
    fun findAllByRestaurant(restaurantId: UUID): List<RecipeSummaryResponse>
    fun findById(id: UUID): RecipeResponse
    fun create(request: CreateRecipeRequest): RecipeResponse
    fun update(id: UUID, request: UpdateRecipeRequest): RecipeResponse
    fun delete(id: UUID)
    fun computeAllergens(recipeId: UUID): List<ComputedAllergen>
}

data class ComputedAllergen(
    val allergenId: Int,
    val allergenCode: String,
    val allergenName: String,
    val containmentLevel: String
)
