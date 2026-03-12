package com.apptolast.menus.recipe.dto.response

data class ComputedAllergenResponse(
    val code: String,
    val level: String,
    val sources: List<String>
)
