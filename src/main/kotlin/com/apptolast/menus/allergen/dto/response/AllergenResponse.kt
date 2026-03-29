package com.apptolast.menus.allergen.dto.response

data class AllergenResponse(
    val id: Int,
    val code: String,
    val nameEs: String = "",
    val nameEn: String = "",
    val iconUrl: String? = null
)
