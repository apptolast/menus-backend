package com.apptolast.menus.recipe.dto.response

import java.util.UUID

data class RecipeSummaryResponse(
    val id: UUID,
    val name: String,
    val category: String?,
    val isSubElaboration: Boolean,
    val isActive: Boolean,
    val allergenCount: Int,
    val componentCount: Int
)
