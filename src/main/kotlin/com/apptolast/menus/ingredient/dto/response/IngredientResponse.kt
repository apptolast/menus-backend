package com.apptolast.menus.ingredient.dto.response

import java.time.OffsetDateTime
import java.util.UUID

data class IngredientResponse(
    val id: UUID,
    val name: String,
    val brand: String = "",
    val supplier: String = "",
    val allergens: List<String> = emptyList(),
    val traces: List<String> = emptyList(),
    val ocrRawText: String? = null,
    val notes: String? = null,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null
)
