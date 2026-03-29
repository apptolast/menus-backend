package com.apptolast.menus.dish.dto.response

data class DishAllergenResponse(
    val allergenId: Int,
    val code: String,
    val name: String = "",
    val containmentLevel: String = "CONTAINS",
    val notes: String = ""
)
