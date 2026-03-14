package com.apptolast.menus.menu.dto.request

import java.util.UUID

data class UpdateMenuRecipesRequest(
    val recipeIds: List<UUID>
)
