package com.apptolast.menus.recipe.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateRecipeRequest(
    @field:NotBlank(message = "Recipe name is required")
    @field:Size(max = 255, message = "Recipe name must not exceed 255 characters")
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val isSubElaboration: Boolean = false,
    val price: BigDecimal? = null,
    val components: List<RecipeComponentRequest> = emptyList()
)
