package com.apptolast.menus.menudigitalcard.repository

import com.apptolast.menus.menudigitalcard.model.entity.MenuDigitalCard
import java.util.UUID

interface MenuDigitalCardRepository {
    fun findByMenuId(menuId: UUID): List<MenuDigitalCard>
    fun findById(id: UUID): MenuDigitalCard?
    fun save(entity: MenuDigitalCard): MenuDigitalCard
    fun deleteById(id: UUID)
    fun existsByMenuIdAndDishId(menuId: UUID, dishId: UUID): Boolean
    fun existsById(id: UUID): Boolean
}
