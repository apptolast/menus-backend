package com.apptolast.menus.menu.repository

import com.apptolast.menus.menu.model.entity.Menu
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface MenuRepository : JpaRepository<Menu, UUID> {
    fun findByRestaurantIdAndIsArchivedFalseOrderByDisplayOrderAsc(restaurantId: UUID): List<Menu>
    fun findByRestaurantIdOrderByDisplayOrderAsc(restaurantId: UUID): List<Menu>
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): Optional<Menu>

    @Query("SELECT COUNT(m) FROM Menu m WHERE m.restaurantId = :restaurantId AND m.isArchived = false")
    fun countActiveMenusByRestaurantId(restaurantId: UUID): Long
}
