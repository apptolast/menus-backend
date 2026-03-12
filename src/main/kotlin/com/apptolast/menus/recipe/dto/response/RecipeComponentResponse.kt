package com.apptolast.menus.recipe.dto.response

import java.math.BigDecimal
import java.util.UUID

data class RecipeComponentResponse(
    val id: UUID,
    val ingredientId: UUID?,
    val ingredientName: String?,
    val subRecipeId: UUID?,
    val subRecipeName: String?,
    val quantity: BigDecimal?,
    val unit: String?,
    val notes: String?,
    val sortOrder: Int
)
