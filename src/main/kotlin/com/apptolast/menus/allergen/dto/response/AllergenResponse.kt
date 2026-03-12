package com.apptolast.menus.allergen.dto.response

data class AllergenResponse(
    val id: Int,
    val code: String,
    val iconUrl: String = "",
    val translations: Map<String, String> = emptyMap()
)
