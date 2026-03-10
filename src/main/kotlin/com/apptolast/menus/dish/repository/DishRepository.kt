package com.apptolast.menus.dish.repository

import com.apptolast.menus.dish.model.entity.Dish
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface DishRepository : JpaRepository<Dish, UUID> {
    fun findBySectionId(sectionId: UUID): List<Dish>
    fun findBySectionMenuRestaurantId(restaurantId: UUID): List<Dish>

    @Query("SELECT COUNT(d) FROM Dish d WHERE d.section.menu.restaurantId = :restaurantId")
    fun countByRestaurantId(restaurantId: UUID): Long

    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.allergens da LEFT JOIN FETCH da.allergen WHERE d.section.id = :sectionId AND d.isAvailable = true")
    fun findWithAllergensBySectionId(sectionId: UUID): List<Dish>
}
