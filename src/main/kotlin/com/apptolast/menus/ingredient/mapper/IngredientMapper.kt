package com.apptolast.menus.ingredient.mapper

import com.apptolast.menus.ingredient.dto.request.CreateIngredientRequest
import com.apptolast.menus.ingredient.dto.request.UpdateIngredientRequest
import com.apptolast.menus.ingredient.dto.response.IngredientAllergenResponse
import com.apptolast.menus.ingredient.dto.response.IngredientResponse
import com.apptolast.menus.ingredient.model.entity.Ingredient
import com.apptolast.menus.ingredient.model.entity.IngredientAllergen

fun Ingredient.toResponse(): IngredientResponse = IngredientResponse(
    id = id,
    name = name,
    description = description,
    brand = brand,
    labelInfo = labelInfo,
    allergens = allergens.map { it.toAllergenResponse() },
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun IngredientAllergen.toAllergenResponse(): IngredientAllergenResponse = IngredientAllergenResponse(
    allergenId = allergen.id,
    allergenCode = allergen.code,
    allergenName = allergen.nameEs,
    containmentLevel = containmentLevel.name
)

fun CreateIngredientRequest.toEntity(): Ingredient = Ingredient(
    name = name,
    description = description,
    brand = brand,
    labelInfo = labelInfo
)

fun UpdateIngredientRequest.applyTo(existing: Ingredient): Ingredient {
    name?.let { existing.name = it }
    description?.let { existing.description = it }
    brand?.let { existing.brand = it }
    labelInfo?.let { existing.labelInfo = it }
    return existing
}
