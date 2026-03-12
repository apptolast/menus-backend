package com.apptolast.menus.dashboard.service.impl

import com.apptolast.menus.dashboard.dto.response.DashboardStatsResponse
import com.apptolast.menus.dashboard.service.DashboardService
import com.apptolast.menus.ingredient.repository.IngredientRepository
import com.apptolast.menus.menu.repository.MenuRepository
import com.apptolast.menus.recipe.repository.RecipeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
@Transactional(readOnly = true)
class DashboardServiceImpl(
    private val ingredientRepository: IngredientRepository,
    private val recipeRepository: RecipeRepository,
    private val menuRepository: MenuRepository,
    private val objectMapper: ObjectMapper
) : DashboardService {

    private val logger = LoggerFactory.getLogger(DashboardServiceImpl::class.java)

    override fun getStats(restaurantId: UUID): DashboardStatsResponse {
        val ingredients = ingredientRepository.findAllByTenantId(restaurantId)
        val recipes = recipeRepository.findAllByRestaurantId(restaurantId)
        val activeRecipes = recipes.count { it.isActive }
        val menus = menuRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId)

        val allergenCounts = mutableMapOf<String, Long>()
        val mapType = object : TypeReference<List<Map<String, String>>>() {}
        for (ingredient in ingredients) {
            try {
                val allergenEntries: List<Map<String, String>> = objectMapper.readValue(ingredient.allergens, mapType)
                for (entry in allergenEntries) {
                    val code = entry["code"] ?: continue
                    allergenCounts[code] = (allergenCounts[code] ?: 0L) + 1L
                }
            } catch (e: Exception) {
                logger.warn("Failed to parse allergens JSONB for ingredient ${ingredient.id}: ${e.message}")
            }
        }

        val allergenFrequency = allergenCounts.mapValues { it.value.toInt() }

        return DashboardStatsResponse(
            totalIngredients = ingredients.size,
            activeRecipes = activeRecipes,
            totalMenus = menus.size,
            totalRestaurants = 1,
            recentActivity = emptyList(),
            allergenFrequency = allergenFrequency
        )
    }
}
