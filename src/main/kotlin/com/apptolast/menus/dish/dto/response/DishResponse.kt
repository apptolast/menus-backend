package com.apptolast.menus.dish.dto.response

import com.apptolast.menus.dish.model.enum.SafetyLevel
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class DishResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val price: BigDecimal?,
    val imageUrl: String?,
    val isAvailable: Boolean,
    val safetyLevel: SafetyLevel?,
    val matchedAllergens: List<String>,
    val allergens: List<DishAllergenResponse>,
    val updatedAt: OffsetDateTime
)
