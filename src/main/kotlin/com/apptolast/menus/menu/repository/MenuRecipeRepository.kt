package com.apptolast.menus.menu.repository

import com.apptolast.menus.menu.model.entity.MenuRecipe
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MenuRecipeRepository : JpaRepository<MenuRecipe, UUID> {
    fun findByMenuIdOrderBySortOrderAsc(menuId: UUID): List<MenuRecipe>
    fun findByRecipeId(recipeId: UUID): List<MenuRecipe>
    fun deleteByMenuIdAndRecipeId(menuId: UUID, recipeId: UUID)
    fun existsByMenuIdAndRecipeId(menuId: UUID, recipeId: UUID): Boolean
}
