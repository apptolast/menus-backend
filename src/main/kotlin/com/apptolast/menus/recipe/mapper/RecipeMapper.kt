package com.apptolast.menus.recipe.mapper

import com.apptolast.menus.recipe.dto.request.CreateRecipeRequest
import com.apptolast.menus.recipe.dto.request.RecipeComponentRequest
import com.apptolast.menus.recipe.dto.response.ComputedAllergenResponse
import com.apptolast.menus.recipe.dto.response.RecipeComponentResponse
import com.apptolast.menus.recipe.dto.response.RecipeResponse
import com.apptolast.menus.recipe.dto.response.RecipeSummaryResponse
import com.apptolast.menus.recipe.model.entity.Recipe
import com.apptolast.menus.recipe.model.entity.RecipeIngredient
import com.apptolast.menus.recipe.service.ComputedAllergen
import com.apptolast.menus.recipe.service.RecipeIngredientInput
import java.util.UUID

fun Recipe.toResponse(allergens: List<ComputedAllergen>): RecipeResponse =
    RecipeResponse(
        id = id,
        name = name,
        description = description ?: "",
        category = category ?: "",
        isSubElaboration = isSubElaboration,
        price = price,
        imageUrl = imageUrl,
        active = isActive,
        allergens = allergens.map { it.toResponse() },
        components = ingredients.map { it.toComponentResponse() },
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun Recipe.toSummaryResponse(allergenCount: Int): RecipeSummaryResponse =
    RecipeSummaryResponse(
        id = id,
        name = name,
        category = category,
        isSubElaboration = isSubElaboration,
        isActive = isActive,
        allergenCount = allergenCount,
        componentCount = ingredients.size
    )

fun RecipeIngredient.toComponentResponse(): RecipeComponentResponse =
    RecipeComponentResponse(
        id = id,
        ingredientId = ingredient?.id,
        ingredientName = ingredient?.name,
        subRecipeId = subRecipe?.id,
        subRecipeName = subRecipe?.name,
        quantity = quantity,
        unit = unit,
        notes = notes,
        sortOrder = sortOrder
    )

fun ComputedAllergen.toResponse(): ComputedAllergenResponse =
    ComputedAllergenResponse(
        code = code,
        level = level,
        sources = sources
    )

fun CreateRecipeRequest.toEntity(
    tenantId: UUID,
    restaurantId: UUID,
    createdBy: UUID?
): Recipe =
    Recipe(
        tenantId = tenantId,
        restaurantId = restaurantId,
        name = name,
        description = description,
        category = category,
        isSubElaboration = isSubElaboration,
        price = price,
        createdBy = createdBy
    )

fun RecipeComponentRequest.toRecipeIngredientInput(): RecipeIngredientInput =
    RecipeIngredientInput(
        ingredientId = ingredientId,
        subRecipeId = subRecipeId,
        quantity = quantity,
        unit = unit,
        notes = notes,
        sortOrder = sortOrder
    )
