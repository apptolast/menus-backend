package com.apptolast.menus.menu.dto.request

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class AddMenuRecipeRequest(
    @field:NotNull val recipeId: UUID,
    val section: String = "General",
    val sortOrder: Int = 0
)
