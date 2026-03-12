package com.apptolast.menus.dashboard.service.impl

import com.apptolast.menus.allergen.model.enum.AllergenType
import com.apptolast.menus.dashboard.dto.response.AllergenStatResponse
import com.apptolast.menus.dashboard.dto.response.DashboardStatsResponse
import com.apptolast.menus.dashboard.service.DashboardService
import com.apptolast.menus.digitalcard.repository.DigitalCardRepository
import com.apptolast.menus.ingredient.repository.IngredientRepository
import com.apptolast.menus.menu.repository.MenuRepository
import com.apptolast.menus.recipe.repository.RecipeRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class DashboardServiceImpl(
    private val ingredientRepository: IngredientRepository,
    private val recipeRepository: RecipeRepository,
    private val menuRepository: MenuRepository,
    private val digitalCardRepository: DigitalCardRepository
) : DashboardService {

    private val logger = LoggerFactory.getLogger(DashboardServiceImpl::class.java)
    private val objectMapper = jacksonObjectMapper()

    override fun getStats(restaurantId: UUID): DashboardStatsResponse {
        val ingredients = ingredientRepository.findAllByTenantId(restaurantId)
        val recipes = recipeRepository.findAllByRestaurantId(restaurantId)
        val activeRecipes = recipes.count { it.isActive }
        val menus = menuRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId)
        val publishedMenus = menus.count { it.isPublished && !it.isArchived }
        val digitalCards = digitalCardRepository.findAllByTenantId(restaurantId)

        val allergenCounts = mutableMapOf<String, Long>()
        for (ingredient in ingredients) {
            try {
                val allergenEntries: List<Map<String, String>> = objectMapper.readValue(ingredient.allergens)
                for (entry in allergenEntries) {
                    val code = entry["code"] ?: continue
                    allergenCounts[code] = (allergenCounts[code] ?: 0L) + 1L
                }
            } catch (e: Exception) {
                logger.warn("Failed to parse allergens JSONB for ingredient ${ingredient.id}: ${e.message}")
            }
        }

        val totalIngredients = ingredients.size.toLong()
        val commonAllergens = allergenCounts.entries
            .sortedByDescending { it.value }
            .map { (code, count) ->
                val allergenType = AllergenType.entries.find { it.name == code }
                AllergenStatResponse(
                    code = code,
                    displayName = allergenType?.displayName ?: code,
                    count = count,
                    percentage = if (totalIngredients > 0) (count.toDouble() / totalIngredients) * 100.0 else 0.0
                )
            }

        return DashboardStatsResponse(
            totalIngredients = totalIngredients,
            activeRecipes = activeRecipes.toLong(),
            totalMenus = menus.size.toLong(),
            publishedMenus = publishedMenus.toLong(),
            totalDigitalCards = digitalCards.size.toLong(),
            commonAllergens = commonAllergens
        )
    }
}
