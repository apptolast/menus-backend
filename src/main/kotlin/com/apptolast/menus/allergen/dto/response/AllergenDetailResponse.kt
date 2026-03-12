package com.apptolast.menus.allergen.dto.response

data class AllergenDetailResponse(
    val id: Int,
    val code: String,
    val iconUrl: String?,
    val translations: List<AllergenTranslationResponse>
)

data class AllergenTranslationResponse(
    val locale: String,
    val name: String,
    val description: String?
)
