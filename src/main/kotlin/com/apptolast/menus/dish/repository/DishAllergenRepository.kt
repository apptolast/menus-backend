package com.apptolast.menus.dish.repository

import com.apptolast.menus.dish.model.entity.DishAllergen
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface DishAllergenRepository : JpaRepository<DishAllergen, UUID> {
    fun findByDishId(dishId: UUID): List<DishAllergen>
    fun findByDishIdAndAllergenId(dishId: UUID, allergenId: Int): Optional<DishAllergen>
    fun deleteByDishIdAndAllergenId(dishId: UUID, allergenId: Int)
}
