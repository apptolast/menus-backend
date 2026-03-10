package com.apptolast.menus.menu.repository

import com.apptolast.menus.menu.model.entity.Menu
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface MenuRepository : JpaRepository<Menu, UUID> {
    @Query("SELECT DISTINCT m FROM Menu m LEFT JOIN FETCH m.sections s WHERE m.restaurantId = :restaurantId AND m.isArchived = false ORDER BY m.displayOrder ASC")
    fun findActiveByRestaurantIdWithSections(restaurantId: UUID): List<Menu>

    @Query("SELECT DISTINCT m FROM Menu m LEFT JOIN FETCH m.sections s WHERE m.restaurantId = :restaurantId ORDER BY m.displayOrder ASC")
    fun findAllByRestaurantIdWithSections(restaurantId: UUID): List<Menu>

    fun findByRestaurantIdAndIsArchivedFalseOrderByDisplayOrderAsc(restaurantId: UUID): List<Menu>
    fun findByRestaurantIdOrderByDisplayOrderAsc(restaurantId: UUID): List<Menu>

    @Query("SELECT COUNT(m) FROM Menu m WHERE m.restaurantId = :restaurantId AND m.isArchived = false")
    fun countActiveMenusByRestaurantId(restaurantId: UUID): Long
}
