package com.apptolast.menus.recipe.dto.response

import java.math.BigDecimal
import java.util.UUID

data class RecipeIngredientResponse(
    val ingredientId: UUID,
    val ingredientName: String,
    val quantity: BigDecimal?,
    val unit: String?
)
