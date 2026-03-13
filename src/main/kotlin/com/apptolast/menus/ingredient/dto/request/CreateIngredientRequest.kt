package com.apptolast.menus.ingredient.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateIngredientRequest(
    @field:NotBlank(message = "Ingredient name is required")
    @field:Size(max = 255, message = "Name must not exceed 255 characters")
    val name: String,

    val description: String? = null,

    @field:Size(max = 255, message = "Brand must not exceed 255 characters")
    val brand: String? = null,

    val labelInfo: String? = null,

    val allergens: List<IngredientAllergenRequest> = emptyList()
)
