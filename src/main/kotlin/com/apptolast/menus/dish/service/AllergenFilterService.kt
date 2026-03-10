package com.apptolast.menus.dish.service

import com.apptolast.menus.dish.model.entity.DishAllergen
import com.apptolast.menus.dish.model.enum.SafetyLevel

interface AllergenFilterService {
    fun computeSafetyLevel(dishAllergens: List<DishAllergen>, userAllergenCodes: List<String>): SafetyLevel
    fun getMatchedAllergens(dishAllergens: List<DishAllergen>, userAllergenCodes: List<String>): List<String>
}
