package com.apptolast.menus.menudigitalcard.repository.jpa

import com.apptolast.menus.menudigitalcard.model.entity.MenuDigitalCard
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JpaMenuDigitalCardRepository : JpaRepository<MenuDigitalCard, UUID> {
    fun findByMenuId(menuId: UUID): List<MenuDigitalCard>
    fun existsByMenuIdAndDishId(menuId: UUID, dishId: UUID): Boolean
}
