package com.apptolast.menus.dashboard.dto.response

data class DashboardStatsResponse(
    val totalIngredients: Int = 0,
    val activeRecipes: Int = 0,
    val totalMenus: Int = 0,
    val totalRestaurants: Int = 0,
    val recentActivity: List<ActivityEntryResponse> = emptyList(),
    val allergenFrequency: Map<String, Int> = emptyMap()
)

data class ActivityEntryResponse(
    val id: String = "",
    val type: String = "",
    val description: String = "",
    val timestamp: String? = null
)
