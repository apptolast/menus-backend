package com.apptolast.menus.ingredient.dto.response

data class AnalyzeTextResponse(
    val detectedAllergens: List<DetectedAllergenResponse>,
    val rawText: String
)

data class DetectedAllergenResponse(
    val code: String,
    val level: String,
    val matchedKeyword: String
)
