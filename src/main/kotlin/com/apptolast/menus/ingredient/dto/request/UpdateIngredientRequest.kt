package com.apptolast.menus.ingredient.dto.request

import jakarta.validation.constraints.Size

data class UpdateIngredientRequest(
    @field:Size(max = 255, message = "Name must not exceed 255 characters")
    val name: String? = null,

    @field:Size(max = 255, message = "Brand must not exceed 255 characters")
    val brand: String? = null,

    @field:Size(max = 255, message = "Supplier must not exceed 255 characters")
    val supplier: String? = null,

    val allergens: List<String>? = null,

    val traces: List<String>? = null,

    val notes: String? = null
)
