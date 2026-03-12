package com.apptolast.menus.recipe.dto.request

import jakarta.validation.constraints.Size
import java.util.UUID

data class UpdateRecipeRequest(
    @field:Size(max = 255, message = "Recipe name must not exceed 255 characters")
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    val active: Boolean? = null,
    val ingredients: List<RecipeIngredientRequest>? = null
)
