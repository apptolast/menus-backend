package com.apptolast.menus.ingredient.dto.response

data class AnalyzeTextResponse(
    val allergens: List<String> = emptyList(),
    val traces: List<String> = emptyList()
)
