package com.apptolast.menus.recipe.mapper

import com.apptolast.menus.recipe.dto.request.CreateRecipeRequest
import com.apptolast.menus.recipe.dto.response.ComputedAllergenResponse
import com.apptolast.menus.recipe.dto.response.RecipeIngredientResponse
import com.apptolast.menus.recipe.dto.response.RecipeResponse
import com.apptolast.menus.recipe.dto.response.RecipeSummaryResponse
import com.apptolast.menus.recipe.model.entity.Recipe
import com.apptolast.menus.recipe.model.entity.RecipeIngredient
import com.apptolast.menus.recipe.service.ComputedAllergen

fun Recipe.toResponse(computedAllergens: List<ComputedAllergen> = emptyList()): RecipeResponse =
    RecipeResponse(
        id = id,
        restaurantId = restaurantId,
        name = name,
        description = description,
        category = category,
        price = price,
        active = active,
        ingredients = ingredients.map { it.toIngredientResponse() },
        computedAllergens = computedAllergens.map { it.toResponse() },
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun Recipe.toSummaryResponse(allergenCount: Int = 0): RecipeSummaryResponse =
    RecipeSummaryResponse(
        id = id,
        name = name,
        category = category,
        price = price,
        active = active,
        ingredientCount = ingredients.size,
        allergenCount = allergenCount
    )

fun RecipeIngredient.toIngredientResponse(): RecipeIngredientResponse =
    RecipeIngredientResponse(
        ingredientId = ingredient.id,
        ingredientName = ingredient.name,
        quantity = quantity,
        unit = unit
    )

fun ComputedAllergen.toResponse(): ComputedAllergenResponse =
    ComputedAllergenResponse(
        allergenId = allergenId,
        allergenCode = allergenCode,
        allergenName = allergenName,
        containmentLevel = containmentLevel
    )

fun CreateRecipeRequest.toEntity(): Recipe =
    Recipe(
        restaurantId = restaurantId,
        name = name,
        description = description,
        category = category,
        price = price
    )
