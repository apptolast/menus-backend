package com.apptolast.menus.menu.repository

import com.apptolast.menus.menu.model.entity.Menu
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface MenuRepository : JpaRepository<Menu, UUID> {

    @Query("SELECT DISTINCT m FROM Menu m LEFT JOIN FETCH m.sections WHERE m.restaurantId = :restaurantId")
    fun findByRestaurantIdWithSections(restaurantId: UUID): List<Menu>

    @Query("SELECT DISTINCT m FROM Menu m LEFT JOIN FETCH m.sections WHERE m.restaurantId = :restaurantId AND m.archived = false")
    fun findByRestaurantIdAndArchivedFalseWithSections(restaurantId: UUID): List<Menu>

    @Query("SELECT DISTINCT m FROM Menu m LEFT JOIN FETCH m.sections WHERE m.restaurantId = :restaurantId AND m.published = true AND m.archived = false")
    fun findByRestaurantIdAndPublishedTrueAndArchivedFalseWithSections(restaurantId: UUID): List<Menu>
}
