package com.apptolast.menus.ingredient.mapper

import com.apptolast.menus.ingredient.dto.request.CreateIngredientRequest
import com.apptolast.menus.ingredient.dto.request.UpdateIngredientRequest
import com.apptolast.menus.ingredient.dto.response.AnalyzeTextResponse
import com.apptolast.menus.ingredient.dto.response.DetectedAllergenResponse
import com.apptolast.menus.ingredient.dto.response.IngredientResponse
import com.apptolast.menus.ingredient.model.entity.Ingredient
import com.apptolast.menus.ingredient.service.DetectedAllergen
import com.apptolast.menus.ingredient.service.TextAnalysisResult
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.util.UUID

fun Ingredient.toResponse(objectMapper: ObjectMapper): IngredientResponse {
    val allergenList: List<String> = parseJsonStringList(allergens, objectMapper)
    val traceList: List<String> = parseJsonStringList(traces ?: "[]", objectMapper)

    return IngredientResponse(
        id = id,
        name = name,
        brand = brand,
        supplier = supplier,
        allergens = allergenList,
        traces = traceList,
        ocrRawText = ocrRawText,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun CreateIngredientRequest.toEntity(
    tenantId: UUID,
    createdBy: UUID?,
    objectMapper: ObjectMapper
): Ingredient = Ingredient(
    tenantId = tenantId,
    name = name,
    brand = brand,
    supplier = supplier,
    allergens = objectMapper.writeValueAsString(allergens),
    traces = objectMapper.writeValueAsString(traces),
    ocrRawText = ocrRawText,
    notes = notes,
    createdBy = createdBy
)

fun UpdateIngredientRequest.applyTo(
    existing: Ingredient,
    objectMapper: ObjectMapper
): Ingredient {
    name?.let { existing.name = it }
    brand?.let { existing.brand = it }
    supplier?.let { existing.supplier = it }
    allergens?.let { existing.allergens = objectMapper.writeValueAsString(it) }
    traces?.let { existing.traces = objectMapper.writeValueAsString(it) }
    notes?.let { existing.notes = it }
    return existing
}

fun TextAnalysisResult.toResponse(): AnalyzeTextResponse = AnalyzeTextResponse(
    detectedAllergens = detectedAllergens.map { it.toResponse() },
    rawText = rawText
)

fun DetectedAllergen.toResponse(): DetectedAllergenResponse = DetectedAllergenResponse(
    code = code,
    level = level,
    matchedKeyword = matchedKeyword
)

private fun parseJsonStringList(json: String, objectMapper: ObjectMapper): List<String> =
    try {
        objectMapper.readValue(json, object : TypeReference<List<String>>() {})
    } catch (_: Exception) {
        emptyList()
    }
