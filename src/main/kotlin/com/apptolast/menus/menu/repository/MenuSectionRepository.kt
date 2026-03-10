package com.apptolast.menus.menu.repository

import com.apptolast.menus.menu.model.entity.MenuSection
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface MenuSectionRepository : JpaRepository<MenuSection, UUID> {
    fun findByMenuIdOrderByDisplayOrderAsc(menuId: UUID): List<MenuSection>
    fun findByIdAndMenuId(sectionId: UUID, menuId: UUID): Optional<MenuSection>
    fun existsByIdAndMenuId(sectionId: UUID, menuId: UUID): Boolean
    fun existsByIdAndMenuRestaurantId(sectionId: UUID, restaurantId: UUID): Boolean
}
