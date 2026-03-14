package com.apptolast.menus.menu.repository

import com.apptolast.menus.menu.model.entity.MenuRecipe
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MenuRecipeRepository : JpaRepository<MenuRecipe, UUID> {
    fun deleteByMenuId(menuId: UUID)
    fun findByMenuId(menuId: UUID): List<MenuRecipe>
}
