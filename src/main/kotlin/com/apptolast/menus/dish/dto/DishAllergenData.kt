package com.apptolast.menus.dish.dto

import com.apptolast.menus.dish.model.enum.ContainmentLevel

data class DishAllergenData(
    val allergenCode: String,
    val containmentLevel: ContainmentLevel
)
