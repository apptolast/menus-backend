package com.apptolast.menus.allergen.repository

import com.apptolast.menus.allergen.model.entity.Allergen
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface AllergenRepository : JpaRepository<Allergen, Int> {
    fun findByCode(code: String): Optional<Allergen>

    @Query("SELECT a FROM Allergen a LEFT JOIN FETCH a.translations WHERE a.code = :code")
    fun findByCodeWithTranslations(code: String): Optional<Allergen>

    @Query("SELECT DISTINCT a FROM Allergen a LEFT JOIN FETCH a.translations ORDER BY a.id")
    fun findAllWithTranslations(): List<Allergen>
}
