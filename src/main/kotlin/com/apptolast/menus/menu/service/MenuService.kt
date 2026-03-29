package com.apptolast.menus.menu.service

import com.apptolast.menus.menu.dto.request.MenuRequest
import com.apptolast.menus.menu.dto.request.SectionRequest
import com.apptolast.menus.menu.dto.response.MenuResponse
import com.apptolast.menus.menu.dto.response.SectionResponse
import java.util.UUID

interface MenuService {
    fun findByRestaurant(restaurantId: UUID, includeArchived: Boolean): List<MenuResponse>
    fun create(restaurantId: UUID, request: MenuRequest): MenuResponse
    fun update(id: UUID, request: MenuRequest): MenuResponse
    fun archive(id: UUID)
    fun addSection(menuId: UUID, request: SectionRequest): SectionResponse
    fun updateSection(sectionId: UUID, request: SectionRequest): SectionResponse
    fun deleteSection(sectionId: UUID)
    fun publish(menuId: UUID, published: Boolean): MenuResponse
    fun updateRecipes(menuId: UUID, recipeIds: List<UUID>): MenuResponse
}
