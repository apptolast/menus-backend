package com.apptolast.menus.dish.service.impl

import com.apptolast.menus.dish.model.entity.DishAllergen
import com.apptolast.menus.dish.model.enum.ContainmentLevel
import com.apptolast.menus.dish.model.enum.SafetyLevel
import com.apptolast.menus.dish.service.AllergenFilterService
import org.springframework.stereotype.Service

@Service
class AllergenFilterServiceImpl : AllergenFilterService {

    /**
     * Semaforo algorithm:
     * - DANGER: any user allergen is CONTAINS in this dish
     * - RISK:   any user allergen is MAY_CONTAIN (and none are CONTAINS)
     * - SAFE:   no user allergen matches at CONTAINS or MAY_CONTAIN level
     */
    override fun computeSafetyLevel(
        dishAllergens: List<DishAllergen>,
        userAllergenCodes: List<String>
    ): SafetyLevel {
        val relevantAllergens = dishAllergens.filter { da ->
            da.allergen.code in userAllergenCodes
        }
        return when {
            relevantAllergens.any { it.containmentLevel == ContainmentLevel.CONTAINS } -> SafetyLevel.DANGER
            relevantAllergens.any { it.containmentLevel == ContainmentLevel.MAY_CONTAIN } -> SafetyLevel.RISK
            else -> SafetyLevel.SAFE
        }
    }

    override fun getMatchedAllergens(
        dishAllergens: List<DishAllergen>,
        userAllergenCodes: List<String>
    ): List<String> =
        dishAllergens
            .filter { da -> da.allergen.code in userAllergenCodes }
            .map { it.allergen.code }
}
