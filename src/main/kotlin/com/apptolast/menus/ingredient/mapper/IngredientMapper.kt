package com.apptolast.menus.ingredient.mapper

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
