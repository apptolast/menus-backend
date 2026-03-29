package com.apptolast.menus.ingredient.dto.response

data class IngredientAllergenResponse(
    val allergenId: Int,
    val allergenCode: String,
    val allergenName: String,
    val containmentLevel: String
)
