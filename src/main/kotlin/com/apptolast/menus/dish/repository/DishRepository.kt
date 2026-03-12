package com.apptolast.menus.dish.repository

import com.apptolast.menus.dish.model.entity.Dish
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface DishRepository : JpaRepository<Dish, UUID> {
    fun findBySectionId(sectionId: UUID): List<Dish>
    fun findBySectionIdOrderByDisplayOrderAsc(sectionId: UUID): List<Dish>
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.allergens LEFT JOIN FETCH d.recipe WHERE d.section.id = :sectionId ORDER BY d.displayOrder ASC")
    fun findBySectionIdWithAllergensAndRecipe(sectionId: UUID): List<Dish>
    @Query("SELECT d FROM Dish d WHERE d.section.menu.restaurantId = :restaurantId ORDER BY d.displayOrder ASC")
    fun findByRestaurantId(restaurantId: UUID): List<Dish>
}
