package com.apptolast.menus.recipe.dto.response

import java.time.OffsetDateTime
import java.util.UUID

data class RecipeResponse(
    val id: UUID,
    val restaurantId: UUID,
    val name: String,
    val description: String?,
    val category: String?,
    val active: Boolean,
    val ingredients: List<RecipeIngredientResponse> = emptyList(),
    val computedAllergens: List<ComputedAllergenResponse> = emptyList(),
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)

data class ComputedAllergenResponse(
    val allergenId: Int,
    val allergenCode: String,
    val allergenName: String,
    val containmentLevel: String
)
