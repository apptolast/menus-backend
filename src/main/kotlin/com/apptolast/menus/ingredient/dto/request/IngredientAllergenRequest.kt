package com.apptolast.menus.ingredient.dto.request

import jakarta.validation.constraints.NotBlank

data class IngredientAllergenRequest(
    @field:NotBlank(message = "Allergen code is required")
    val allergenCode: String,
    @field:NotBlank(message = "Containment level is required")
    val containmentLevel: String
)
