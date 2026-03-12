package com.apptolast.menus.recipe.dto.response

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class RecipeResponse(
    val id: UUID,
    val name: String,
    val description: String = "",
    val category: String = "",
    val isSubElaboration: Boolean,
    val price: BigDecimal?,
    val imageUrl: String?,
    val active: Boolean,
    val allergens: List<ComputedAllergenResponse>,
    val components: List<RecipeComponentResponse>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)
