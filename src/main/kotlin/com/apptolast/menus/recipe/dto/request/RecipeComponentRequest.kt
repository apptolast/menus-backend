package com.apptolast.menus.recipe.dto.request

import java.math.BigDecimal
import java.util.UUID

data class RecipeComponentRequest(
    val ingredientId: UUID? = null,
    val subRecipeId: UUID? = null,
    val quantity: BigDecimal? = null,
    val unit: String? = null,
    val notes: String? = null,
    val sortOrder: Int = 0
)
