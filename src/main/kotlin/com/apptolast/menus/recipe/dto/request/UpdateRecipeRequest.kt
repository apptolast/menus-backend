package com.apptolast.menus.recipe.dto.request

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

data class UpdateRecipeRequest(
    @field:Size(max = 255, message = "Recipe name must not exceed 255 characters")
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    @field:DecimalMin(value = "0.00", message = "Price must be >= 0")
    val price: BigDecimal? = null,
    val active: Boolean? = null,
    val ingredients: List<RecipeIngredientRequest>? = null
)
