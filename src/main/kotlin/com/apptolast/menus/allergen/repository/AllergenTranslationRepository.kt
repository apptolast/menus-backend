package com.apptolast.menus.allergen.repository

import com.apptolast.menus.allergen.model.entity.AllergenTranslation
import org.springframework.data.jpa.repository.JpaRepository

interface AllergenTranslationRepository : JpaRepository<AllergenTranslation, Int> {
    fun findByAllergenIdAndLocale(allergenId: Int, locale: String): AllergenTranslation?
    fun findByLocale(locale: String): List<AllergenTranslation>
}
