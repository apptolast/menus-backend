package com.apptolast.menus.dashboard.dto.response

data class DashboardStatsResponse(
    val totalIngredients: Long,
    val activeRecipes: Long,
    val totalMenus: Long,
    val publishedMenus: Long,
    val totalDigitalCards: Long,
    val commonAllergens: List<AllergenStatResponse>
)

data class AllergenStatResponse(
    val code: String,
    val displayName: String,
    val count: Long,
    val percentage: Double
)
