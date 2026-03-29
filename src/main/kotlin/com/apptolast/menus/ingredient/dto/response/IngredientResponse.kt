package com.apptolast.menus.ingredient.dto.response

import java.time.OffsetDateTime
import java.util.UUID

data class IngredientResponse(
    val id: UUID,
    val name: String,
    val description: String? = null,
    val brand: String? = null,
    val labelInfo: String? = null,
    val allergens: List<IngredientAllergenResponse> = emptyList(),
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null
)
