package com.apptolast.menus.menu.repository

import com.apptolast.menus.menu.model.entity.MenuSection
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface MenuSectionRepository : JpaRepository<MenuSection, UUID> {
    fun findByMenuIdOrderByDisplayOrderAsc(menuId: UUID): List<MenuSection>
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): Optional<MenuSection>
    fun existsByIdAndTenantId(id: UUID, tenantId: UUID): Boolean
}
