package com.apptolast.menus.dish.service

import com.apptolast.menus.dish.dto.DishAllergenData
import com.apptolast.menus.dish.model.enum.SafetyLevel

interface AllergenFilterService {
    fun computeSafetyLevel(dishAllergens: List<DishAllergenData>, userAllergenCodes: List<String>): SafetyLevel
    fun getMatchedAllergens(dishAllergens: List<DishAllergenData>, userAllergenCodes: List<String>): List<String>
}
