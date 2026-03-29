package com.apptolast.menus.menu.repository

import com.apptolast.menus.menu.model.entity.MenuSection
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MenuSectionRepository : JpaRepository<MenuSection, UUID> {
    fun findByMenuId(menuId: UUID): List<MenuSection>
    fun findByMenuIdOrderByDisplayOrderAsc(menuId: UUID): List<MenuSection>
}
