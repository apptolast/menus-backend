package com.apptolast.menus.menu.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class MenuRequest(
    @field:NotBlank @field:Size(max = 255) val name: String,
    val description: String? = null,
    val displayOrder: Int = 0,
    @field:Size(max = 500) val restaurantLogoUrl: String? = null,
    @field:Size(max = 500) val companyLogoUrl: String? = null,
    val recipeIds: List<UUID>? = null
)
