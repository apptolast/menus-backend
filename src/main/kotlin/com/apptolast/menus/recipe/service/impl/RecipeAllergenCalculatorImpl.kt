package com.apptolast.menus.recipe.service.impl

import com.apptolast.menus.ingredient.repository.IngredientRepository
import com.apptolast.menus.recipe.repository.RecipeIngredientRepository
import com.apptolast.menus.recipe.repository.RecipeRepository
import com.apptolast.menus.recipe.service.ComputedAllergen
import com.apptolast.menus.recipe.service.RecipeAllergenCalculator
import com.apptolast.menus.shared.exception.CyclicRecipeException
import com.apptolast.menus.shared.exception.MaxDepthExceededException
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RecipeAllergenCalculatorImpl(
    private val recipeRepository: RecipeRepository,
    private val recipeIngredientRepository: RecipeIngredientRepository,
    private val ingredientRepository: IngredientRepository,
    private val objectMapper: ObjectMapper
) : RecipeAllergenCalculator {

    private val log = LoggerFactory.getLogger(RecipeAllergenCalculatorImpl::class.java)

    companion object {
        private const val MAX_DEPTH = 10

        /** Priority order for containment levels: higher value = higher priority */
        private val LEVEL_PRIORITY = mapOf(
            "CONTAINS" to 3,
            "MAY_CONTAIN" to 2,
            "FREE_OF" to 1
        )
    }

    override fun computeAllergens(recipeId: UUID): List<ComputedAllergen> {
        log.info("Computing allergens for recipe {}", recipeId)
        val allergenMap = mutableMapOf<String, AllergenAccumulator>()
        computeAllergensRecursive(recipeId, mutableSetOf(), 0, allergenMap, emptyList())

        return allergenMap.values
            .filter { it.level != "FREE_OF" }
            .map { acc ->
                ComputedAllergen(
                    code = acc.code,
                    level = acc.level,
                    sources = acc.sources.toList()
                )
            }
            .sortedBy { it.code }
    }

    private fun computeAllergensRecursive(
        recipeId: UUID,
        visited: MutableSet<UUID>,
        depth: Int,
        allergenMap: MutableMap<String, AllergenAccumulator>,
        parentTrail: List<String>
    ) {
        if (depth > MAX_DEPTH) {
            throw MaxDepthExceededException(
                "Sub-recipe nesting exceeds maximum depth of $MAX_DEPTH (recipe $recipeId)"
            )
        }

        if (!visited.add(recipeId)) {
            throw CyclicRecipeException(
                "Cyclic dependency detected at recipe $recipeId"
            )
        }

        try {
            val recipeIngredients = recipeIngredientRepository.findByRecipeId(recipeId)

            for (ri in recipeIngredients) {
                val ingredient = ri.ingredient
                val subRecipe = ri.subRecipe

                if (ingredient != null) {
                    val sourceName = ingredient.name
                    val trail = if (parentTrail.isEmpty()) sourceName
                    else "${parentTrail.joinToString(" > ")} > $sourceName"

                    // Parse allergens JSONB
                    parseAllergens(ingredient.allergens, "CONTAINS").forEach { parsed ->
                        mergeAllergen(allergenMap, parsed.code, parsed.level, trail)
                    }

                    // Parse traces JSONB
                    val tracesJson = ingredient.traces
                    if (tracesJson != null) {
                        parseTraces(tracesJson).forEach { parsed ->
                            mergeAllergen(allergenMap, parsed.code, "MAY_CONTAIN", trail)
                        }
                    }
                } else if (subRecipe != null) {
                    val subRecipeName = subRecipe.name
                    val newTrail = parentTrail + subRecipeName
                    computeAllergensRecursive(subRecipe.id, visited, depth + 1, allergenMap, newTrail)
                }
            }
        } finally {
            visited.remove(recipeId)
        }
    }

    override fun detectCycle(recipeId: UUID, newSubRecipeId: UUID): Boolean {
        log.debug("Checking for cycle: adding sub-recipe {} to recipe {}", newSubRecipeId, recipeId)
        if (recipeId == newSubRecipeId) {
            return true
        }
        return checkForCycleRecursive(recipeId, newSubRecipeId, mutableSetOf(), 0)
    }

    private fun checkForCycleRecursive(
        targetRecipeId: UUID,
        currentRecipeId: UUID,
        visited: MutableSet<UUID>,
        depth: Int
    ): Boolean {
        if (depth > MAX_DEPTH) {
            return false
        }

        if (!visited.add(currentRecipeId)) {
            return false
        }

        try {
            val ingredients = recipeIngredientRepository.findByRecipeId(currentRecipeId)

            for (ri in ingredients) {
                val subRecipe = ri.subRecipe ?: continue
                if (subRecipe.id == targetRecipeId) {
                    return true
                }
                if (checkForCycleRecursive(targetRecipeId, subRecipe.id, visited, depth + 1)) {
                    return true
                }
            }

            return false
        } finally {
            visited.remove(currentRecipeId)
        }
    }

    /**
     * Parses the allergens JSONB field.
     * Expected format: [{"code":"GLUTEN","level":"CONTAINS"}, ...]
     */
    private fun parseAllergens(allergensJson: String, defaultLevel: String): List<ParsedAllergen> {
        if (allergensJson.isBlank() || allergensJson == "[]") {
            return emptyList()
        }
        return try {
            val items: List<AllergenJsonEntry> = objectMapper.readValue(allergensJson)
            items.map { entry ->
                ParsedAllergen(
                    code = entry.code,
                    level = entry.level ?: defaultLevel
                )
            }
        } catch (e: Exception) {
            log.warn("Failed to parse allergens JSON: {}", e.message)
            emptyList()
        }
    }

    /**
     * Parses the traces JSONB field.
     * Expected format: [{"code":"SOYBEANS","source":"shared equipment"}, ...]
     */
    private fun parseTraces(tracesJson: String): List<ParsedAllergen> {
        if (tracesJson.isBlank() || tracesJson == "[]") {
            return emptyList()
        }
        return try {
            val items: List<TraceJsonEntry> = objectMapper.readValue(tracesJson)
            items.map { entry ->
                ParsedAllergen(
                    code = entry.code,
                    level = "MAY_CONTAIN"
                )
            }
        } catch (e: Exception) {
            log.warn("Failed to parse traces JSON: {}", e.message)
            emptyList()
        }
    }

    private fun mergeAllergen(
        map: MutableMap<String, AllergenAccumulator>,
        code: String,
        level: String,
        source: String
    ) {
        val existing = map[code]
        if (existing == null) {
            map[code] = AllergenAccumulator(code, level, mutableListOf(source))
        } else {
            if (!existing.sources.contains(source)) {
                existing.sources.add(source)
            }
            val existingPriority = LEVEL_PRIORITY[existing.level] ?: 0
            val newPriority = LEVEL_PRIORITY[level] ?: 0
            if (newPriority > existingPriority) {
                existing.level = level
            }
        }
    }

    private data class AllergenAccumulator(
        val code: String,
        var level: String,
        val sources: MutableList<String>
    )

    private data class ParsedAllergen(
        val code: String,
        val level: String
    )

    private data class AllergenJsonEntry(
        val code: String = "",
        val level: String? = null
    )

    private data class TraceJsonEntry(
        val code: String = "",
        val source: String? = null
    )
}
