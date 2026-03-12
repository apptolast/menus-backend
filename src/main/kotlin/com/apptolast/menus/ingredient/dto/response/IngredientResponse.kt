package com.apptolast.menus.ingredient.dto.response

import java.time.OffsetDateTime
import java.util.UUID

data class IngredientResponse(
    val id: UUID,
    val name: String,
    val brand: String?,
    val supplier: String?,
    val allergens: List<String>,
    val traces: List<String>,
    val ocrRawText: String?,
    val notes: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)
