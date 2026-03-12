package com.apptolast.menus.recipe.dto.request

import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class UpdateRecipeRequest(
    @field:Size(max = 255, message = "Recipe name must not exceed 255 characters")
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    val isSubElaboration: Boolean? = null,
    val price: BigDecimal? = null,
    val isActive: Boolean? = null,
    val components: List<RecipeComponentRequest>? = null
)
