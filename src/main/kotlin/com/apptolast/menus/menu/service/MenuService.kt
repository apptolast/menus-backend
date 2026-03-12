package com.apptolast.menus.menu.service

import com.apptolast.menus.menu.dto.request.MenuRequest
import com.apptolast.menus.menu.dto.request.SectionRequest
import com.apptolast.menus.menu.dto.response.AllergenMatrixResponse
import com.apptolast.menus.menu.dto.response.MenuResponse
import com.apptolast.menus.menu.dto.response.SectionResponse
import com.apptolast.menus.menu.model.entity.MenuRecipe
import java.util.UUID

interface MenuService {
    fun findByRestaurant(restaurantId: UUID, includeArchived: Boolean): List<MenuResponse>
    fun create(restaurantId: UUID, tenantId: UUID, request: MenuRequest): MenuResponse
    fun update(id: UUID, tenantId: UUID, request: MenuRequest): MenuResponse
    fun archive(id: UUID, tenantId: UUID)
    fun addSection(menuId: UUID, tenantId: UUID, request: SectionRequest): SectionResponse
    fun updateSection(sectionId: UUID, tenantId: UUID, request: SectionRequest): SectionResponse
    fun deleteSection(sectionId: UUID, tenantId: UUID)
    fun publish(menuId: UUID, tenantId: UUID, published: Boolean): MenuResponse
    fun addRecipeToMenu(menuId: UUID, tenantId: UUID, recipeId: UUID, section: String, sortOrder: Int): MenuRecipe
    fun removeRecipeFromMenu(menuId: UUID, tenantId: UUID, recipeId: UUID)
    fun getAllergenMatrix(menuId: UUID, tenantId: UUID): AllergenMatrixResponse
    fun exportPdf(menuId: UUID, tenantId: UUID): ByteArray
}
