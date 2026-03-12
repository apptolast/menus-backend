package com.apptolast.menus.menu.dto.response

import java.util.UUID

data class AllergenMatrixResponse(
    val menuName: String,
    val menuId: UUID,
    val allergenColumns: List<String>,
    val rows: List<AllergenMatrixRow>
)

data class AllergenMatrixRow(
    val recipeId: UUID,
    val recipeName: String,
    val section: String,
    val allergens: Map<String, String>
)
