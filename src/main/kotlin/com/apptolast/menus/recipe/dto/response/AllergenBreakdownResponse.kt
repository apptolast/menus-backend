package com.apptolast.menus.recipe.dto.response

import java.util.UUID

data class AllergenBreakdownResponse(
    val recipeId: UUID,
    val recipeName: String,
    val allergens: List<ComputedAllergenResponse>,
    val components: List<ComponentAllergenDetail>
)

data class ComponentAllergenDetail(
    val componentName: String,
    val isSubRecipe: Boolean,
    val allergens: List<ComputedAllergenResponse>
)
