package com.apptolast.menus.ingredient.service

interface TextAnalyzerService {
    fun analyzeText(text: String): TextAnalysisResult
}

data class TextAnalysisResult(
    val detectedAllergens: List<DetectedAllergen>,
    val rawText: String
)

data class DetectedAllergen(
    val code: String,
    val level: String,
    val matchedKeyword: String
)
