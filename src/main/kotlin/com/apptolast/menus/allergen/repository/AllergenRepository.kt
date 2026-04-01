package com.apptolast.menus.allergen.repository

import com.apptolast.menus.allergen.model.entity.Allergen
import org.springframework.data.jpa.repository.JpaRepository

interface AllergenRepository : JpaRepository<Allergen, Int> {
    fun findByCode(code: String): Allergen?
    fun findAllByCodeIn(codes: List<String>): List<Allergen>
}
