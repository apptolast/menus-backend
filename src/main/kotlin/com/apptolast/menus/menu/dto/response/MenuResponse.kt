package com.apptolast.menus.menu.dto.response

import java.time.OffsetDateTime
import java.util.UUID

data class MenuResponse(
    val id: UUID,
    val name: String,
    val description: String = "",
    val published: Boolean,
    val archived: Boolean,
    val displayOrder: Int,
    val sections: List<SectionResponse> = emptyList(),
    val restaurantLogoUrl: String? = null,
    val companyLogoUrl: String? = null,
    val recipes: List<MenuRecipeResponse> = emptyList(),
    val updatedAt: OffsetDateTime
)
