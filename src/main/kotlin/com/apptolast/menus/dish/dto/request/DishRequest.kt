package com.apptolast.menus.dish.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class DishRequest(
    @field:NotBlank @field:Size(max = 255) val name: String,
    val sectionId: UUID,
    val description: String? = null,
    val imageUrl: String? = null,
    val available: Boolean = true,
    val displayOrder: Int? = 0,
    val allergens: List<DishAllergenRequest> = emptyList()
)

data class DishAllergenRequest(
    val allergenCode: String,
    val containmentLevel: String,
    val notes: String? = null
)
