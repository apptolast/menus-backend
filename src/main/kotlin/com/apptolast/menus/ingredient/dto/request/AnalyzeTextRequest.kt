package com.apptolast.menus.ingredient.dto.request

import jakarta.validation.constraints.NotBlank

data class AnalyzeTextRequest(
    @field:NotBlank(message = "Text to analyze is required")
    val text: String
)
