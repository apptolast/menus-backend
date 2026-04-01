package com.apptolast.menus.recipe.dto.request

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

data class CreateRecipeRequest(
    @field:NotNull(message = "Restaurant ID is required")
    val restaurantId: UUID,

    @field:NotBlank(message = "Recipe name is required")
    @field:Size(max = 255, message = "Recipe name must not exceed 255 characters")
    val name: String,

    val description: String? = null,

    val category: String? = null,

    @field:DecimalMin(value = "0.00", message = "Price must be >= 0")
    val price: BigDecimal? = null,

    val ingredients: List<RecipeIngredientRequest> = emptyList()
)

data class RecipeIngredientRequest(
    val ingredientId: UUID,
    val quantity: java.math.BigDecimal? = null,
    val unit: String? = null
)
