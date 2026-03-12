package com.apptolast.menus.menu.repository

import com.apptolast.menus.menu.model.entity.Menu
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MenuRepository : JpaRepository<Menu, UUID> {
    fun findByRestaurantId(restaurantId: UUID): List<Menu>
    fun findByRestaurantIdAndArchivedFalse(restaurantId: UUID): List<Menu>
    fun findByRestaurantIdAndPublishedTrueAndArchivedFalse(restaurantId: UUID): List<Menu>
}
