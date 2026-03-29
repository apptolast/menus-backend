package com.apptolast.menus.recipe.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateRecipeRequest(
    @field:NotNull(message = "Restaurant ID is required")
    val restaurantId: UUID,

    @field:NotBlank(message = "Recipe name is required")
    @field:Size(max = 255, message = "Recipe name must not exceed 255 characters")
    val name: String,

    val description: String? = null,

    val category: String? = null,

    val ingredients: List<RecipeIngredientRequest> = emptyList()
)

data class RecipeIngredientRequest(
    val ingredientId: UUID,
    val quantity: java.math.BigDecimal? = null,
    val unit: String? = null
)
