package com.apptolast.menus.dish.service.impl

import com.apptolast.menus.dish.dto.DishAllergenData
import com.apptolast.menus.dish.model.enum.ContainmentLevel
import com.apptolast.menus.dish.model.enum.SafetyLevel
import com.apptolast.menus.dish.service.AllergenFilterService
import org.springframework.stereotype.Service

@Service
class AllergenFilterServiceImpl : AllergenFilterService {

    override fun computeSafetyLevel(
        dishAllergens: List<DishAllergenData>,
        userAllergenCodes: List<String>
    ): SafetyLevel {
        val relevantAllergens = dishAllergens.filter { da ->
            da.allergenCode in userAllergenCodes
        }
        return when {
            relevantAllergens.any { it.containmentLevel == ContainmentLevel.CONTAINS } -> SafetyLevel.DANGER
            relevantAllergens.any { it.containmentLevel == ContainmentLevel.MAY_CONTAIN } -> SafetyLevel.RISK
            else -> SafetyLevel.SAFE
        }
    }

    override fun getMatchedAllergens(
        dishAllergens: List<DishAllergenData>,
        userAllergenCodes: List<String>
    ): List<String> =
        dishAllergens
            .filter { da -> da.allergenCode in userAllergenCodes }
            .map { it.allergenCode }
}
